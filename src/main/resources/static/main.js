const chat = document.getElementById('chat');
const input = document.getElementById('messageInput');
const sendBtn = document.getElementById('sendBtn');
const chatList = document.getElementById('chatList');
const fileLoader = document.getElementById('fileLoader');
const helpModal = document.getElementById('helpModal');
const overlay = document.getElementById('overlay');

const ws = new WebSocket("ws://localhost:8080/ws/chat");

let chats = {};
let currentChatId = null;

function newChat() {
    const name = prompt("Enter chat name:");
    if (!name) return;
    const id = 'chat-' + Date.now();
    chats[id] = {name, messages: []};
    addChatTab(id);
    switchChat(id);
}

function addChatTab(id) {
    const btn = document.createElement('button');
    btn.textContent = chats[id].name;
    btn.className = 'chat-tab';
    btn.onclick = () => switchChat(id);
    btn.dataset.id = id;
    chatList.appendChild(btn);
}

function switchChat(id) {
    currentChatId = id;
    [...chatList.children].forEach(b => b.classList.toggle('active', b.dataset.id === id));
    renderChat();
}

function appendMessage(content, sender) {
    if (!currentChatId) return;
    chats[currentChatId].messages.push({sender, content});
    renderChat();
}

function renderChat() {
    if (!currentChatId) return;
    chat.innerHTML = '';
    chats[currentChatId].messages.forEach(msg => {
        const div = document.createElement('div');
        div.className = `message ${msg.sender}`;
        div.textContent = msg.content;
        chat.appendChild(div);
    });
    chat.scrollTop = chat.scrollHeight;
}

sendBtn.addEventListener('click', () => {
    const message = input.value.trim();
    if (message && currentChatId) {
        ws.send(message);
        appendMessage(message, 'user');
        input.value = '';
    }
});

input.addEventListener('keydown', (e) => {
    if (e.key === 'Enter') {
        sendBtn.click();
    }
});

ws.onmessage = function (event) {
    appendMessage(event.data, 'bot');
};

ws.onopen = () => {
    newChat();
};
ws.onclose = () => appendMessage("Connection closed.", 'bot');

function saveToXML() {
    if (!currentChatId) return;
    const messages = chats[currentChatId].messages;
    const xml = messages.map(msg =>
        `<message sender="${msg.sender}">${escapeXML(msg.content)}</message>`
    ).join('');
    const blob = new Blob([`<chat name="${escapeXML(chats[currentChatId].name)}">${xml}</chat>`], {type: 'application/xml'});
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = `${chats[currentChatId].name}.xml`;
    a.click();
}

function escapeXML(str) {
    return str.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
}

fileLoader.addEventListener('change', (e) => {
    const file = e.target.files[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = function () {
        const parser = new DOMParser();
        const xml = parser.parseFromString(reader.result, "text/xml");
        const chatElement = xml.getElementsByTagName("chat")[0];
        const name = chatElement.getAttribute("name") || "Imported Chat";
        const msgs = [...xml.getElementsByTagName("message")];
        const id = 'chat-' + Date.now();
        chats[id] = {
            name,
            messages: msgs.map(el => ({
                sender: el.getAttribute('sender'),
                content: el.textContent
            }))
        };
        addChatTab(id);
        switchChat(id);
    };
    reader.readAsText(file);
});

function openHelp() {
    helpModal.style.display = 'block';
    overlay.style.display = 'block';
}

function closeHelp() {
    helpModal.style.display = 'none';
    overlay.style.display = 'none';
}