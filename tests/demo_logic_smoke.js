// Dependency-free smoke test for the browser demo state machine.
const fs=require('fs'), vm=require('vm');
const html=fs.readFileSync(__dirname+'/../demo/index.html','utf8');
const script=html.split('<script>')[1].split('</script>')[0];
class El {
  constructor(id=''){this.id=id;this.value='';this.textContent='';this.innerHTML='';this.checked=false;this.disabled=false;this.className='';this.style={};this.children=[];this.classList={toggle:()=>{},remove:()=>{},add:()=>{}};this.onclick=null;this.onchange=null;this.handlers={};}
  append(...x){this.children.push(...x)} prepend(...x){this.children.unshift(...x)} addEventListener(n,f){this.handlers[n]=f;}
}
const ids=['ssidInput','addSsid','ssidChips','scanNetworks','scanTime','scanResults','wifiToggle','geoToggle','wifiBadge','geoBadge','prayerBadge','durationRange','durationValue','durationValueTop','radiusRange','radiusValue','currentProfile','heroState','imamEnd','adminBadge','statusBox','statePill','stateTitle','stateDescription','countdown','countdownLabel','progressFill','logicBadge','wifiLogic','geoLogic','timeLogic','wifiLogicText','geoLogicText','timeLogicText','previousProfile','startConditions','resetDemo','iqamaTime','setIqama30','setIqama90','clearIqama','eventLog','clearLog','useLocation','locationStatus'];
function makeContext(){
  const map={};ids.forEach(id=>map[id]=new El(id));map.durationRange.value='10';map.radiusRange.value='150';map.previousProfile.value='Normal';
  let now=Date.now();
  class FakeDate extends Date { constructor(...args){super(...(args.length?args:[now]));} static now(){return now;} }
  const document={getElementById:id=>map[id]||(map[id]=new El(id)),createElement:tag=>new El(tag)};
  const context={document,console,Date:FakeDate,Math,Number,String,Array,JSON,Intl,navigator:{},location:{protocol:'file:'},setInterval:fn=>{context.tick=fn;return 1;},clearInterval:()=>{},setTimeout,clearTimeout};
  vm.runInNewContext(script,context);
  context.map=map;context.setNow=n=>{now=n;};context.now=()=>now;
  return context;
}
function prime(ctx){const m=ctx.map;m.scanNetworks.onclick();m.ssidInput.value='Cafe_Guest_WiFi';m.addSsid.onclick();m.wifiToggle.checked=true;m.wifiToggle.onchange({target:m.wifiToggle});m.geoToggle.checked=true;m.geoToggle.onchange({target:m.geoToggle});}
function assert(cond,msg){if(!cond)throw new Error(msg);}

// 1) Both presence signals and active prayer window activate; Imam release restores and suppresses re-entry.
{
 const c=makeContext(),m=c.map;prime(c);m.setIqama30.onclick();
 assert(m.currentProfile.textContent==='Silent','expected silent activation');
 assert(!m.imamEnd.disabled,'Imam release should be enabled while active');
 m.imamEnd.onclick();assert(m.currentProfile.textContent==='Normal sound','expected original profile restore');
 c.tick();assert(m.currentProfile.textContent==='Normal sound','manual Imam release must not reactivate this event');
}
// 2) Wi-Fi alone cannot activate.
{
 const c=makeContext(),m=c.map;m.scanNetworks.onclick();m.ssidInput.value='Cafe_Guest_WiFi';m.addSsid.onclick();m.wifiToggle.checked=true;m.wifiToggle.onchange({target:m.wifiToggle});m.setIqama30.onclick();
 assert(m.currentProfile.textContent==='Normal sound','Wi-Fi alone should not activate');
}
// 3) Geofence alone cannot activate.
{
 const c=makeContext(),m=c.map;m.geoToggle.checked=true;m.geoToggle.onchange({target:m.geoToggle});m.setIqama30.onclick();
 assert(m.currentProfile.textContent==='Normal sound','geofence alone should not activate');
}
// 4) Losing either presence signal while active restores the saved profile.
{
 const c=makeContext(),m=c.map;prime(c);m.previousProfile.value='Vibrate';m.previousProfile.onchange({target:m.previousProfile});m.setIqama30.onclick();
 assert(m.currentProfile.textContent==='Silent','expected silent activation');
 m.wifiToggle.checked=false;m.wifiToggle.onchange({target:m.wifiToggle});
 assert(m.currentProfile.textContent==='Vibrate','Wi-Fi loss should restore Vibrate');
}
// 5) D=1 minute auto-restores at Iqama + D.
{
 const c=makeContext(),m=c.map;prime(c);m.durationRange.value='1';m.durationRange.oninput();m.setIqama30.onclick();
 assert(m.currentProfile.textContent==='Silent','expected silent activation for auto-restore test');
 c.setNow(c.now()+30_000+60_000);c.tick();
 assert(m.currentProfile.textContent==='Normal sound','automatic restore at Iqama + D failed');
}
// 6) An originally-silent phone is restored to Silent, not forced to Normal.
{
 const c=makeContext(),m=c.map;prime(c);m.previousProfile.value='Silent';m.previousProfile.onchange({target:m.previousProfile});m.setIqama30.onclick();
 assert(m.currentProfile.textContent==='Silent','already-silent profile should remain silent');
 m.imamEnd.onclick();assert(m.currentProfile.textContent==='Silent','original Silent profile must be preserved');
}
console.log('PASS: 6 browser-demo smoke scenarios.');
