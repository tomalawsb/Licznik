(() => {
  "use strict";
  const APP_VERSION = "1.1 - 0906260752";
  const SETTINGS_KEY = "licznik_jazdy_settings_v1";
  const HISTORY_KEY = "licznik_jazdy_history_v1";
  const $ = id => document.getElementById(id);
  const els = {
    gpsStatus: $("gpsStatus"), headerClock: $("headerClock"), currentSpeed: $("currentSpeed"), avgSpeed: $("avgSpeed"),
    distance: $("distance"), rideTime: $("rideTime"), maxSpeed: $("maxSpeed"), pointsCount: $("pointsCount"), accuracyInfo: $("accuracyInfo"),
    routeLine: $("routeLine"), startDot: $("startDot"), endDot: $("endDot"), routeHint: $("routeHint"),
    startBtn: $("startBtn"), pauseBtn: $("pauseBtn"), stopBtn: $("stopBtn"), resetBtn: $("resetBtn"),
    settingsBtn: $("settingsBtn"), settingsDialog: $("settingsDialog"), versionLabel: $("versionLabel"), wakeToggle: $("wakeToggle"),
    toast: $("toast"), historyList: $("historyList"), clearHistoryBtn: $("clearHistoryBtn"), totalRides: $("totalRides"),
    totalDistance: $("totalDistance"), bikeDistance: $("bikeDistance"), carDistance: $("carDistance"), installBtn: $("installBtn")
  };
  let state = { mode:"bike", running:false, paused:false, startTime:0, elapsedBeforePause:0, distanceM:0, maxSpeed:0, currentSpeed:0, lastPoint:null, route:[], watchId:null, wakeWanted:false, wakeLock:null, installPrompt:null };

  function load(){ try{ const s=JSON.parse(localStorage.getItem(SETTINGS_KEY)||"{}"); if(["bike","car"].includes(s.mode)) state.mode=s.mode; state.wakeWanted=!!s.wakeWanted; }catch{} }
  function save(){ localStorage.setItem(SETTINGS_KEY, JSON.stringify({mode:state.mode,wakeWanted:state.wakeWanted})); }
  function history(){ try{return JSON.parse(localStorage.getItem(HISTORY_KEY)||"[]")}catch{return[]} }
  function saveHistory(items){ localStorage.setItem(HISTORY_KEY, JSON.stringify(items)); }
  function toast(msg){ els.toast.textContent=msg; els.toast.classList.add("show"); clearTimeout(toast.t); toast.t=setTimeout(()=>els.toast.classList.remove("show"),3000); }
  function clock(date=new Date()){ return date.toLocaleTimeString("pl-PL",{hour:"2-digit",minute:"2-digit"}); }
  function duration(ms){ const s=Math.max(0,Math.floor(ms/1000)); return [Math.floor(s/3600),Math.floor((s%3600)/60),s%60].map(v=>String(v).padStart(2,"0")).join(":"); }
  function elapsed(){ if(!state.running || state.paused) return state.elapsedBeforePause; return Date.now()-state.startTime; }
  function modeName(){ return state.mode==="car"?"Samochód":"Rower"; }
  function maxReasonable(){ return state.mode==="car"?260:95; }
  function hav(a,b){ const R=6371000, rad=d=>d*Math.PI/180; const dLat=rad(b.lat-a.lat), dLon=rad(b.lon-a.lon), lat1=rad(a.lat), lat2=rad(b.lat); const h=Math.sin(dLat/2)**2+Math.cos(lat1)*Math.cos(lat2)*Math.sin(dLon/2)**2; return 2*R*Math.asin(Math.sqrt(h)); }

  function updateMode(){ document.querySelectorAll(".mode-btn").forEach(b=>b.classList.toggle("active",b.dataset.mode===state.mode)); }
  function update(){
    const ms=elapsed(), km=state.distanceM/1000, avg=(ms>0)?km/(ms/3600000):0;
    els.headerClock.textContent=clock();
    els.currentSpeed.textContent=state.currentSpeed.toFixed(1);
    els.avgSpeed.textContent=avg.toFixed(3);
    els.distance.textContent=km.toFixed(2);
    els.rideTime.textContent=duration(ms);
    els.maxSpeed.textContent=state.maxSpeed.toFixed(1);
    els.pointsCount.textContent=String(state.route.length);
    els.startBtn.disabled=state.running && !state.paused;
    els.pauseBtn.disabled=!state.running;
    els.stopBtn.disabled=!state.running && state.distanceM===0;
    els.pauseBtn.innerHTML=state.paused?"▶<span>Wznów</span>":"⏸<span>Pauza</span>";
    els.gpsStatus.textContent=state.running?(state.paused?"PWA • Pomiar w pauzie":"PWA • GPS aktywny"):"PWA • GPS gotowy";
    drawRoute();
  }
  function drawRoute(){
    if(state.route.length<2){ els.routeLine.setAttribute("points",""); els.startDot.setAttribute("r","0"); els.endDot.setAttribute("r","0"); els.routeHint.textContent=state.route.length?"Zapisano pierwszy punkt GPS. Rusz dalej, żeby zobaczyć trasę.":"Po kliknięciu Start aplikacja zacznie rysować trasę z GPS."; return; }
    const lats=state.route.map(p=>p.lat), lons=state.route.map(p=>p.lon); const minLat=Math.min(...lats), maxLat=Math.max(...lats), minLon=Math.min(...lons), maxLon=Math.max(...lons); const latR=Math.max(.000001,maxLat-minLat), lonR=Math.max(.000001,maxLon-minLon);
    const pts=state.route.map(p=>[10+((p.lon-minLon)/lonR)*340,15+(1-((p.lat-minLat)/latR))*140]);
    els.routeLine.setAttribute("points",pts.map(p=>`${p[0].toFixed(1)},${p[1].toFixed(1)}`).join(" "));
    const first=pts[0], last=pts[pts.length-1];
    els.startDot.setAttribute("cx",first[0]); els.startDot.setAttribute("cy",first[1]); els.startDot.setAttribute("r","7");
    els.endDot.setAttribute("cx",last[0]); els.endDot.setAttribute("cy",last[1]); els.endDot.setAttribute("r","7");
    els.routeHint.textContent="Trasa jest rysowana lokalnie z zapisanych punktów GPS.";
  }
  async function wakeOn(){ if(!state.wakeWanted || !("wakeLock" in navigator)) return; try{ state.wakeLock=await navigator.wakeLock.request("screen"); }catch{ toast("Nie udało się włączyć niewygaszania ekranu."); } }
  async function wakeOff(){ if(state.wakeLock){ try{ await state.wakeLock.release(); }catch{} state.wakeLock=null; } }
  function start(){
    if(!("geolocation" in navigator)){ toast("Brak obsługi GPS w tej przeglądarce."); return; }
    if(!state.running){ Object.assign(state,{running:true,paused:false,startTime:Date.now(),elapsedBeforePause:0,distanceM:0,maxSpeed:0,currentSpeed:0,lastPoint:null,route:[]}); }
    else if(state.paused){ state.paused=false; state.startTime=Date.now()-state.elapsedBeforePause; }
    if(state.watchId===null){ state.watchId=navigator.geolocation.watchPosition(onPos,onErr,{enableHighAccuracy:true,maximumAge:1000,timeout:12000}); }
    wakeOn(); update(); toast("Pomiar rozpoczęty.");
  }
  function pause(){ if(!state.running) return; if(!state.paused){ state.elapsedBeforePause=elapsed(); state.paused=true; state.currentSpeed=0; toast("Pomiar wstrzymany."); } else { state.paused=false; state.startTime=Date.now()-state.elapsedBeforePause; toast("Pomiar wznowiony."); } update(); }
  function stop(){ if(state.watchId!==null){ navigator.geolocation.clearWatch(state.watchId); state.watchId=null; } const ms=elapsed(); state.running=false; state.paused=false; state.currentSpeed=0; state.elapsedBeforePause=ms; wakeOff(); if(ms>=10000 || state.distanceM>=20){ addRide(ms); toast("Przejazd zapisany w historii."); } else toast("Pomiar zatrzymany. Za krótki przejazd nie został zapisany."); renderHistory(); renderStats(); update(); }
  function reset(){ if(state.watchId!==null){ navigator.geolocation.clearWatch(state.watchId); state.watchId=null; } wakeOff(); Object.assign(state,{running:false,paused:false,startTime:0,elapsedBeforePause:0,distanceM:0,maxSpeed:0,currentSpeed:0,lastPoint:null,route:[]}); els.accuracyInfo.textContent="Dokładność: --"; update(); toast("Licznik wyzerowany."); }
  function onPos(pos){
    if(!state.running || state.paused) return;
    const c=pos.coords, acc=Number.isFinite(c.accuracy)?c.accuracy:null, p={lat:c.latitude,lon:c.longitude,time:pos.timestamp||Date.now(),accuracy:acc};
    if(acc!==null) els.accuracyInfo.textContent=`Dokładność: ${acc.toFixed(0)} m`;
    if(!state.lastPoint){ state.lastPoint=p; state.route.push(p); update(); return; }
    const step=hav(state.lastPoint,p), sec=Math.max(.1,(p.time-state.lastPoint.time)/1000), computed=(step/sec)*3.6, sensor=(Number.isFinite(c.speed)&&c.speed>=0)?c.speed*3.6:null, speed=sensor??computed;
    if((acc===null||acc<=80) && step>=3 && computed<=maxReasonable()){ state.distanceM+=step; state.lastPoint=p; state.route.push(p); if(state.route.length>900) state.route=state.route.slice(-900); }
    else if(step<3) state.lastPoint=p;
    state.currentSpeed=Math.max(0,Math.min(speed,maxReasonable())); state.maxSpeed=Math.max(state.maxSpeed,state.currentSpeed); update();
  }
  function onErr(e){ const msg={1:"Brak zgody na lokalizację.",2:"Nie udało się ustalić lokalizacji GPS.",3:"GPS nie odpowiedział w wymaganym czasie."}; toast(msg[e.code]||"Błąd GPS."); els.gpsStatus.textContent="PWA • błąd GPS"; }
  function addRide(ms){ const km=state.distanceM/1000, avg=ms?km/(ms/3600000):0; const item={id:String(Date.now()),mode:state.mode,date:new Date().toISOString(),durationMs:ms,distanceM:state.distanceM,avgSpeedKmh:avg,maxSpeedKmh:state.maxSpeed}; const h=history(); h.unshift(item); saveHistory(h.slice(0,100)); }
  function renderHistory(){ const h=history(); if(!h.length){ els.historyList.innerHTML='<div class="empty">Brak zapisanych przejazdów.</div>'; return; } els.historyList.innerHTML=h.map(i=>{ const km=i.distanceM/1000, d=new Date(i.date), cls=i.mode==="car"?"car":"bike", icon=i.mode==="car"?"🚗":"🚲", name=i.mode==="car"?"Samochód":"Rower"; return `<article class="history-item"><div class="history-head"><div class="mode-pill ${cls}">${icon}</div><div><div class="history-title">${name}</div><div class="history-date">${d.toLocaleDateString("pl-PL")} ${clock(d)}</div></div><div class="history-distance">${km.toFixed(2)} km</div></div><div class="history-meta"><span>◷ ${duration(i.durationMs)}</span><span>śr. ${i.avgSpeedKmh.toFixed(3)} km/h</span><span>maks. ${i.maxSpeedKmh.toFixed(1)} km/h</span></div></article>`; }).join(""); }
  function renderStats(){ const h=history(), sum=arr=>arr.reduce((a,i)=>a+i.distanceM,0)/1000; els.totalRides.textContent=String(h.length); els.totalDistance.textContent=sum(h).toFixed(2)+" km"; els.bikeDistance.textContent=sum(h.filter(i=>i.mode==="bike")).toFixed(2)+" km"; els.carDistance.textContent=sum(h.filter(i=>i.mode==="car")).toFixed(2)+" km"; }
  function screen(id){ document.querySelectorAll(".screen").forEach(s=>s.classList.toggle("active",s.id===id)); document.querySelectorAll(".nav").forEach(n=>n.classList.toggle("active",n.dataset.screen===id)); if(id==="screenHistory") renderHistory(); if(id==="screenStats") renderStats(); }
  function bind(){
    document.querySelectorAll(".mode-btn").forEach(b=>b.onclick=()=>{ state.mode=b.dataset.mode; save(); updateMode(); toast("Tryb jazdy: "+modeName()); });
    document.querySelectorAll(".nav").forEach(b=>b.onclick=()=>screen(b.dataset.screen));
    els.startBtn.onclick=start; els.pauseBtn.onclick=pause; els.stopBtn.onclick=stop; els.resetBtn.onclick=reset; els.settingsBtn.onclick=()=>els.settingsDialog.showModal();
    els.wakeToggle.onchange=()=>{ state.wakeWanted=els.wakeToggle.checked; save(); state.wakeWanted?wakeOn():wakeOff(); toast(state.wakeWanted?"Włączono niewygaszanie ekranu.":"Wyłączono niewygaszanie ekranu."); };
    els.clearHistoryBtn.onclick=()=>{ if(confirm("Usunąć całą historię jazdy?")){ saveHistory([]); renderHistory(); renderStats(); toast("Historia wyczyszczona."); } };
    window.addEventListener("beforeinstallprompt", e=>{ e.preventDefault(); state.installPrompt=e; els.installBtn.hidden=false; });
    els.installBtn.onclick=async()=>{ if(!state.installPrompt) return; state.installPrompt.prompt(); await state.installPrompt.userChoice; state.installPrompt=null; els.installBtn.hidden=true; };
    document.addEventListener("visibilitychange",()=>{ if(document.visibilityState==="visible" && state.running && !state.paused) wakeOn(); });
  }
  function sw(){ if("serviceWorker" in navigator) window.addEventListener("load",()=>navigator.serviceWorker.register("./service-worker.js").catch(console.warn)); }
  function init(){ load(); els.versionLabel.textContent=APP_VERSION; els.wakeToggle.checked=state.wakeWanted; updateMode(); bind(); renderHistory(); renderStats(); update(); setInterval(update,1000); sw(); }
  init();
})();
