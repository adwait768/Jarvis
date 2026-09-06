// ---------- State ----------
let tasks = [];
let notes = [];
let chatHistory = [];
let voiceOut = false;
let apiKey = '';

let recognition = null;
let listening = false;

let handsFree = false;
let wakeWord = 'hey trevor';
let hfRecognition = null;
let hfState = 'idle'; // idle | awaiting | thinking | speaking
let suspendHF = false;

// ---------- Storage (plain localStorage — this is a normal website, not a Claude artifact) ----------
function loadJSON(key, fallback){
  try{ const v = localStorage.getItem(key); return v ? JSON.parse(v) : fallback; }
  catch(e){ return fallback; }
}
function saveJSON(key, val){
  try{ localStorage.setItem(key, JSON.stringify(val)); }
  catch(e){ console.error('save failed', key, e); }
}

function loadAll(){
  tasks = loadJSON('trevor-tasks', []);
  notes = loadJSON('trevor-notes', []);
  chatHistory = loadJSON('trevor-chat', []);
  voiceOut = loadJSON('trevor-voiceout', false);
  handsFree = loadJSON('trevor-handsfree', false);
  wakeWord = loadJSON('trevor-wakeword', 'hey trevor');
  apiKey = loadJSON('trevor-apikey', '');

  renderTasks(); renderNotes(); renderChat(); renderVoiceSwitch(); renderHFSwitch();
  document.getElementById('wakeWordInput').value = wakeWord;
  document.getElementById('apiKeyInput').value = apiKey;
  if(handsFree) startHF();

  if('serviceWorker' in navigator){
    navigator.serviceWorker.register('sw.js').catch(()=>{});
  }
}

// ---------- Tabs ----------
function switchTab(tab){
  document.querySelectorAll('.view').forEach(v=>v.classList.remove('active'));
  document.querySelectorAll('.tabbtn').forEach(b=>b.classList.remove('active'));
  document.getElementById(tab+'View').classList.add('active');
  document.querySelector('.tabbtn[data-tab="'+tab+'"]').classList.add('active');
  document.getElementById('chatInputBar').style.display = (tab==='chat') ? 'flex' : 'none';
  const subs = {chat:'your daily assistant', tasks:'stays on this device', notes:'stays on this device', settings:'preferences'};
  document.getElementById('headerSub').textContent = subs[tab];
}

// ---------- Chat rendering ----------
function renderChat(){
  const el = document.getElementById('chatView');
  el.innerHTML = '';
  if(chatHistory.length===0){
    el.innerHTML = '<div class="msg system">Say hello, or ask me to help with your tasks and notes.</div>';
    return;
  }
  chatHistory.forEach(m=>{
    const d = document.createElement('div');
    d.className = 'msg ' + m.role;
    d.textContent = m.content;
    el.appendChild(d);
  });
  el.scrollTop = el.scrollHeight;
}

// ---------- AI call ----------
async function fetchAIReply(excludeMsg){
  if(!apiKey){
    throw new Error('no-key');
  }
  const context = 'Current tasks: ' + (tasks.filter(t=>!t.done).map(t=>t.text).join('; ') || 'none') +
                   '. Current notes: ' + (notes.map(n=>n.text).join('; ') || 'none') + '.';
  const apiMessages = chatHistory
    .filter(m=>m!==excludeMsg)
    .slice(-12)
    .map(m=>({role: m.role==='assistant' ? 'assistant' : 'user', content: m.content}));

  const response = await fetch('https://api.anthropic.com/v1/messages', {
    method:'POST',
    headers:{
      'Content-Type':'application/json',
      'x-api-key': apiKey,
      'anthropic-version': '2023-06-01',
      'anthropic-dangerous-direct-browser-access': 'true'
    },
    body: JSON.stringify({
      model:'claude-sonnet-4-6',
      max_tokens:1000,
      system:'You are Trevor, a concise, friendly personal daily-assistant living in a small phone app, sometimes reached hands-free by voice. ' + context + ' Keep replies short and practical, mobile-screen length, and easy to read aloud. If the user asks to add/remove a task or note, tell them to use the Tasks/Notes tab, since you cannot edit them directly.',
      messages: apiMessages
    })
  });
  if(!response.ok){
    const errBody = await response.text().catch(()=> '');
    throw new Error('api-error: ' + response.status + ' ' + errBody);
  }
  const data = await response.json();
  let reply = '(no response)';
  if(data && data.content){
    reply = data.content.map(b=>b.text||'').join('\n').trim() || reply;
  }
  return reply;
}

async function sendMessage(){
  const input = document.getElementById('chatInput');
  const text = input.value.trim();
  if(!text) return;
  input.value = '';
  chatHistory.push({role:'user', content:text});
  renderChat();
  saveJSON('trevor-chat', chatHistory);

  if(!apiKey){
    chatHistory.push({role:'assistant', content:'Add your Anthropic API key in Settings to enable chat replies. Voice, tasks and notes work without one.'});
    renderChat();
    saveJSON('trevor-chat', chatHistory);
    return;
  }

  const thinking = {role:'assistant', content:'…'};
  chatHistory.push(thinking);
  renderChat();

  try{
    const reply = await fetchAIReply(thinking);
    thinking.content = reply;
    renderChat();
    saveJSON('trevor-chat', chatHistory);
    if(voiceOut) speak(reply);
  }catch(e){
    thinking.content = "I couldn't reach the assistant — check your API key in Settings and your connection.";
    renderChat();
    saveJSON('trevor-chat', chatHistory);
  }
}

// ---------- Manual tap-to-talk ----------
function setupRecognition(){
  const SR = window.SpeechRecognition || window.webkitSpeechRecognition;
  if(!SR) return null;
  const r = new SR();
  r.lang = 'en-US';
  r.interimResults = false;
  r.maxAlternatives = 1;
  r.onresult = (e)=>{
    const transcript = e.results[0][0].transcript;
    document.getElementById('chatInput').value = transcript;
    sendMessage();
  };
  r.onend = ()=>{ listening = false; document.getElementById('micBtn').classList.remove('listening'); };
  r.onerror = ()=>{ listening = false; document.getElementById('micBtn').classList.remove('listening'); };
  return r;
}
function toggleMic(){
  if(!recognition) recognition = setupRecognition();
  if(!recognition){ alert('Voice input is not supported in this browser. Try Chrome on Android.'); return; }
  if(listening){ recognition.stop(); listening = false; document.getElementById('micBtn').classList.remove('listening'); return; }
  try{
    recognition.start();
    listening = true;
    document.getElementById('micBtn').classList.add('listening');
  }catch(e){}
}
function speak(text){
  try{
    if(!('speechSynthesis' in window)) return;
    window.speechSynthesis.cancel();
    const u = new SpeechSynthesisUtterance(text);
    window.speechSynthesis.speak(u);
  }catch(e){}
}
function toggleVoiceOut(){
  voiceOut = !voiceOut;
  renderVoiceSwitch();
  saveJSON('trevor-voiceout', voiceOut);
}
function renderVoiceSwitch(){
  document.getElementById('voiceOutSwitch').classList.toggle('on', voiceOut);
}
function setApiKey(v){
  apiKey = v.trim();
  saveJSON('trevor-apikey', apiKey);
}

// ---------- Hands-free "Hey Trevor" mode ----------
function renderHFSwitch(){
  document.getElementById('hfSwitch').classList.toggle('on', handsFree);
}
function setWakeWord(v){
  const w = v.trim().toLowerCase();
  wakeWord = w || 'hey trevor';
  document.getElementById('wakeWordInput').value = wakeWord;
  saveJSON('trevor-wakeword', wakeWord);
}
function updateHFIndicator(state){
  hfState = state;
  const el = document.getElementById('hfStatus');
  if(!handsFree || !state){ el.style.display = 'none'; return; }
  el.style.display = 'block';
  const labels = {
    idle: 'Listening for "' + wakeWord + '"',
    awaiting: 'Yes? Go ahead…',
    thinking: 'Thinking…',
    speaking: 'Speaking…'
  };
  el.textContent = labels[state] || '';
}
function toggleHandsFree(){
  handsFree = !handsFree;
  renderHFSwitch();
  saveJSON('trevor-handsfree', handsFree);
  if(handsFree){ startHF(); } else { stopHF(); }
}
function startHF(){
  const SR = window.SpeechRecognition || window.webkitSpeechRecognition;
  if(!SR){
    alert('Voice recognition isn\'t supported in this browser. Try Chrome on Android.');
    handsFree = false; renderHFSwitch(); return;
  }
  if(!window.isSecureContext){
    alert('Voice recognition needs HTTPS. Make sure you\'re opening the GitHub Pages link (https://...), not a local file.');
    handsFree = false; renderHFSwitch(); return;
  }
  suspendHF = false;
  hfRecognition = new SR();
  hfRecognition.lang = 'en-US';
  hfRecognition.continuous = true;
  hfRecognition.interimResults = false;
  hfRecognition.onresult = (e)=>{
    const res = e.results[e.results.length-1];
    if(res && res[0]) handleHFTranscript(res[0].transcript.trim());
  };
  hfRecognition.onend = ()=>{
    if(handsFree && !suspendHF){
      try{ hfRecognition.start(); }catch(err){}
    }
  };
  hfRecognition.onerror = (e)=>{
    if(e.error === 'not-allowed' || e.error === 'service-not-allowed'){
      handsFree = false; renderHFSwitch(); updateHFIndicator(null);
      alert('Microphone access is blocked. Check your browser\'s site permissions and allow the microphone for this page.');
    }
  };
  try{ hfRecognition.start(); updateHFIndicator('idle'); }catch(err){}
}
function stopHF(){
  suspendHF = true;
  if(hfRecognition){ try{ hfRecognition.stop(); }catch(e){} }
  updateHFIndicator(null);
}
function handleHFTranscript(transcript){
  const lower = transcript.toLowerCase();
  if(hfState === 'awaiting'){
    runHFCommand(transcript);
    return;
  }
  const idx = lower.indexOf(wakeWord);
  if(idx === -1) return;
  const after = transcript.slice(idx + wakeWord.length).trim();
  if(after.length > 0){
    runHFCommand(after);
  }else{
    updateHFIndicator('awaiting');
  }
}
async function runHFCommand(text){
  updateHFIndicator('thinking');
  chatHistory.push({role:'user', content:text});
  renderChat(); saveJSON('trevor-chat', chatHistory);
  const thinking = {role:'assistant', content:'…'};
  chatHistory.push(thinking); renderChat();

  let reply;
  if(!apiKey){
    reply = 'Add your Anthropic API key in Settings so I can answer.';
  }else{
    try{ reply = await fetchAIReply(thinking); }
    catch(e){ reply = "I couldn't reach the assistant just now."; }
  }
  thinking.content = reply;
  renderChat(); saveJSON('trevor-chat', chatHistory);

  updateHFIndicator('speaking');
  suspendHF = true;
  if(hfRecognition){ try{ hfRecognition.stop(); }catch(e){} }
  speakThen(reply, ()=>{
    suspendHF = false;
    updateHFIndicator('idle');
    if(handsFree){ try{ hfRecognition.start(); }catch(e){} }
  });
}
function speakThen(text, onDone){
  try{
    if(!('speechSynthesis' in window)){ onDone(); return; }
    window.speechSynthesis.cancel();
    const u = new SpeechSynthesisUtterance(text);
    u.onend = onDone;
    u.onerror = onDone;
    window.speechSynthesis.speak(u);
  }catch(e){ onDone(); }
}

// ---------- Tasks ----------
function renderTasks(){
  const el = document.getElementById('taskList');
  el.innerHTML = '';
  if(tasks.length===0){ el.innerHTML = '<div class="empty">No tasks yet — add your first one above.</div>'; return; }
  tasks.forEach((t,i)=>{
    const row = document.createElement('div');
    row.className = 'listRow' + (t.done ? ' done' : '');
    row.innerHTML = '<div class="check">'+(t.done?'✓':'')+'</div><div class="rowtext"></div><div class="rowdel">✕</div>';
    row.querySelector('.rowtext').textContent = t.text;
    row.querySelector('.check').onclick = ()=>{ tasks[i].done = !tasks[i].done; saveJSON('trevor-tasks', tasks); renderTasks(); };
    row.querySelector('.rowdel').onclick = ()=>{ tasks.splice(i,1); saveJSON('trevor-tasks', tasks); renderTasks(); };
    el.appendChild(row);
  });
}
function addTask(){
  const input = document.getElementById('taskInput');
  const text = input.value.trim();
  if(!text) return;
  tasks.unshift({text, done:false});
  input.value = '';
  saveJSON('trevor-tasks', tasks);
  renderTasks();
}

// ---------- Notes ----------
function renderNotes(){
  const el = document.getElementById('noteList');
  el.innerHTML = '';
  if(notes.length===0){ el.innerHTML = '<div class="empty">No notes yet — jot something down above.</div>'; return; }
  notes.forEach((n,i)=>{
    const row = document.createElement('div');
    row.className = 'listRow';
    row.innerHTML = '<div class="rowtext" style="margin-left:2px;"></div><div class="rowdel">✕</div>';
    row.querySelector('.rowtext').textContent = n.text;
    row.querySelector('.rowdel').onclick = ()=>{ notes.splice(i,1); saveJSON('trevor-notes', notes); renderNotes(); };
    el.appendChild(row);
  });
}
function addNote(){
  const input = document.getElementById('noteInput');
  const text = input.value.trim();
  if(!text) return;
  notes.unshift({text});
  input.value = '';
  saveJSON('trevor-notes', notes);
  renderNotes();
}

// ---------- Settings ----------
function clearAll(){
  if(!confirm('This clears tasks, notes, chat and your API key on this device. Continue?')) return;
  tasks = []; notes = []; chatHistory = []; apiKey = '';
  saveJSON('trevor-tasks', tasks);
  saveJSON('trevor-notes', notes);
  saveJSON('trevor-chat', chatHistory);
  saveJSON('trevor-apikey', apiKey);
  document.getElementById('apiKeyInput').value = '';
  renderTasks(); renderNotes(); renderChat();
}

document.getElementById('taskInput').addEventListener('keydown', e=>{ if(e.key==='Enter') addTask(); });
document.getElementById('noteInput').addEventListener('keydown', e=>{ if(e.key==='Enter') addNote(); });

loadAll();
