const screens = {
  landing: document.getElementById('landing'),
  login: document.getElementById('login'),
  otp: document.getElementById('otp'),
  portal: document.getElementById('portal')
};

const modal = document.getElementById('modal');
const modalTitle = document.getElementById('modal-title');
const modalBody = document.getElementById('modal-body');

const state = {
  role: 'student',
  pendingStudentId: '',
  currentPage: 'dashboard',
  currentChat: 'Ahmed A.',
  chatOpen: false,
  studentChatInput: '',
  studentChatMessages: [
    { sender: 'support', text: 'Hello! How can I help you today?', time: '10:30 AM' }
  ]
};

const iconFiles = {
  bell: 'icon-bell.png',
  dashboard: 'icon-home.png',
  visa: 'icon-visa.png',
  documents: 'icon-documents.png',
  appointments: 'icon-appointments.png',
  chat: 'icon-chat.png',
  medical: 'icon-medical.png',
  avatarStudent: 'icon-avatar-student.png'
};

function renderIcon(key, className = '') {
  const fileName = iconFiles[key];
  if (!fileName) {
    return '<span class="icon-placeholder"></span>';
  }
  return `<img src="./assets/${fileName}" class="icon-img ${className}" alt="" onerror="this.style.visibility='hidden'" />`;
}

const staffMenu = [
  ['dashboard', 'Dashboard'],
  ['students', 'Student Records'],
  ['visa', 'Visa & Medical Processing'],
  ['documents', 'Document Verification'],
  ['appointments', 'Appointments Management'],
  ['chat', 'Chatbot & Live Chat'],
  ['reports', 'Reports & Analytics']
];

const studentMenu = [
  ['dashboard', 'Dashboard'],
  ['visa', 'Visa & Medical Status'],
  ['documents', 'Documents'],
  ['appointments', 'Appointments']
];

async function apiPost(path, payload) {
  const response = await fetch(path, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  });

  const data = await response.json().catch(() => ({}));
  if (!response.ok) {
    throw new Error(data.error || data.message || 'Request failed');
  }
  return data;
}

function showScreen(name) {
  Object.values(screens).forEach((screen) => screen.classList.remove('active'));
  screens[name].classList.add('active');
}

function setRole(role) {
  state.role = role;
  document.getElementById('tab-student').classList.toggle('active', role === 'student');
  document.getElementById('tab-staff').classList.toggle('active', role === 'staff');
  document.getElementById('login-btn').textContent = role === 'student' ? 'Login as Student' : 'Login as Staff';
  document.getElementById('studentId').value = '';
  document.getElementById('password').value = '';
  document.getElementById('login-id-label').textContent = role === 'student' ? 'Student ID' : 'Staff ID';
  document.getElementById('studentId').placeholder = role === 'student' ? 'Enter your student ID' : 'Enter your staff ID';
}

function openModal(title, body) {
  modalTitle.textContent = title;
  modalBody.textContent = body;
  modal.classList.remove('hidden');
}

document.getElementById('modal-cancel').onclick = () => modal.classList.add('hidden');
document.getElementById('modal-ok').onclick = () => modal.classList.add('hidden');
document.getElementById('go-login').onclick = (event) => {
  event.preventDefault();
  showScreen('login');
};

document.querySelectorAll('[data-role]').forEach((button) => {
  button.onclick = () => {
    setRole(button.dataset.role);
    showScreen('login');
  };
});

document.getElementById('tab-student').onclick = () => setRole('student');
document.getElementById('tab-staff').onclick = () => setRole('staff');

document.getElementById('login-btn').onclick = async () => {
  const id = document.getElementById('studentId').value.trim();
  const pass = document.getElementById('password').value.trim();
  const errorNode = document.getElementById('login-error');

  if (!id || !pass) {
    errorNode.textContent = 'Please enter ID and password';
    return;
  }

  if (pass.length < 6) {
    errorNode.textContent = 'Password must be at least 6 characters';
    return;
  }

  try {
    errorNode.textContent = '';
    const data = await apiPost('/api/auth/login', { studentId: id, password: pass });

    state.pendingStudentId = data.studentId || id;
    document.getElementById('otp-hint').textContent = "We've sent a 6-digit code to your email";
    document.querySelectorAll('.otp-inputs input').forEach((input) => {
      input.value = '';
    });
    document.getElementById('otp-error').textContent = '';
    showScreen('otp');
    document.querySelector('.otp-inputs input')?.focus();
  } catch (error) {
    errorNode.textContent = error.message;
  }
};

document.getElementById('verify-btn').onclick = async () => {
  const code = [...document.querySelectorAll('.otp-inputs input')]
    .map((input) => input.value.trim())
    .join('');
  const errorNode = document.getElementById('otp-error');

  if (!state.pendingStudentId) {
    errorNode.textContent = 'Please login first';
    showScreen('login');
    return;
  }

  if (code.length !== 6) {
    errorNode.textContent = 'Please enter complete 6-digit OTP';
    return;
  }

  try {
    errorNode.textContent = '';
    const data = await apiPost('/api/auth/login/verify-otp', {
      studentId: state.pendingStudentId,
      otp: code
    });

    if (data.accessToken) {
      localStorage.setItem('accessToken', data.accessToken);
    }
    if (data.refreshToken) {
      localStorage.setItem('refreshToken', data.refreshToken);
    }
    localStorage.setItem('studentId', state.pendingStudentId);
    openPortal();
  } catch (error) {
    errorNode.textContent = error.message;
  }
};

document.getElementById('resend').onclick = async (event) => {
  event.preventDefault();

  if (!state.pendingStudentId) {
    document.getElementById('otp-error').textContent = 'Please login first';
    return;
  }

  try {
    const data = await apiPost('/api/auth/resend-otp', { studentId: state.pendingStudentId });
    openModal('Resend OTP', data.message || 'OTP has been resent.');
  } catch (error) {
    document.getElementById('otp-error').textContent = error.message;
  }
};

function openPortal() {
  showScreen('portal');
  state.currentPage = 'dashboard';
  renderShell();
  renderPage();
}

function renderShell() {
  const menuRoot = document.getElementById('menu');
  const portal = document.getElementById('portal');
  const isStaff = state.role === 'staff';
  const menu = isStaff ? staffMenu : studentMenu;

  portal.classList.toggle('student-mode', !isStaff);
  portal.classList.toggle('staff-mode', isStaff);

  document.getElementById('portal-type').textContent = isStaff ? 'Staff Portal' : 'Student Portal';
  document.getElementById('portal-title').textContent = isStaff ? 'International Office Portal' : 'Student Portal';
  document.getElementById('portal-subtitle').textContent = isStaff ? 'Administrative Dashboard' : '';
  document.getElementById('user-name').textContent = isStaff ? 'Officer Lim' : 'Ahmed A.';
  document.getElementById('user-role').textContent = isStaff ? 'Administrator' : '';
  const userChip = document.querySelector('.user-chip');
  if (isStaff) {
    document.querySelector('.avatar').textContent = 'O';
    document.querySelector('.notif').textContent = '3';
  } else {
    userChip.innerHTML = `
      <span class="bell-wrap">${renderIcon('bell', 'bell-icon')}<span class="notif">3</span></span>
      <div class="avatar">${renderIcon('avatarStudent', 'avatar-icon')}</div>
      <div><b id="user-name">Ahmed A.</b><small id="user-role"></small></div>
    `;
  }
  userChip.classList.toggle('student-chip', !isStaff);
  userChip.classList.toggle('staff-chip', isStaff);

  menuRoot.innerHTML = menu
    .map(([key, label]) => {
      const icon = renderIcon(key, 'menu-svg');
      const activeClass = state.currentPage === key ? 'active' : '';
      return `<button class="menu-item ${activeClass}" data-page="${key}"><span class="menu-icon">${icon}</span><span>${label}</span></button>`;
    })
    .join('');

  menuRoot.querySelectorAll('.menu-item').forEach((item) => {
    item.onclick = () => {
      state.currentPage = item.dataset.page;
      renderShell();
      renderPage();
    };
  });

  renderStudentChatWidget();
}

function renderStudentChatWidget() {
  const root = document.getElementById('student-chat-root');
  if (state.role !== 'student') {
    root.innerHTML = '';
    return;
  }

  const messagesHtml = state.studentChatMessages
    .map((message) => {
      const bubbleClass = message.sender === 'user' ? 'student-chat-user' : 'student-chat-support';
      return `<div class="student-chat-message ${bubbleClass}"><p>${message.text}</p><small>${message.time}</small></div>`;
    })
    .join('');

  root.innerHTML = `
    <button id="student-chat-toggle" class="student-chat-toggle" aria-label="Open chat support">
      ${renderIcon('chat', 'chat-launch-icon')}
    </button>
    <section class="student-chat-panel ${state.chatOpen ? 'open' : ''}">
      <header>
        <h3>Support Chat</h3>
        <button id="student-chat-close" aria-label="Close chat">×</button>
      </header>
      <div class="student-chat-messages">${messagesHtml}</div>
      <footer>
        <input id="student-chat-input" placeholder="Type a message..." value="${state.studentChatInput}" />
        <button id="student-chat-send" aria-label="Send message">➤</button>
      </footer>
    </section>
  `;

  root.querySelector('#student-chat-toggle').onclick = () => {
    state.chatOpen = !state.chatOpen;
    renderStudentChatWidget();
  };

  root.querySelector('#student-chat-close').onclick = () => {
    state.chatOpen = false;
    renderStudentChatWidget();
  };

  const input = root.querySelector('#student-chat-input');
  input.oninput = () => {
    state.studentChatInput = input.value;
  };

  const send = () => {
    const text = state.studentChatInput.trim();
    if (!text) {
      return;
    }
    state.studentChatMessages.push({ sender: 'user', text, time: 'Now' });
    state.studentChatInput = '';
    state.chatOpen = true;
    renderStudentChatWidget();
  };

  root.querySelector('#student-chat-send').onclick = send;
  input.onkeydown = (event) => {
    if (event.key === 'Enter') {
      event.preventDefault();
      send();
    }
  };
}

function renderPage() {
  const root = document.getElementById('page-content');
  const isStaff = state.role === 'staff';
  const page = state.currentPage;

  if (isStaff) {
    if (page === 'dashboard') {
      root.innerHTML = staffDashboard();
      return;
    }
    if (page === 'students') {
      root.innerHTML = staffStudents();
      return;
    }
    if (page === 'visa') {
      root.innerHTML = staffVisa();
      return;
    }
    if (page === 'documents') {
      root.innerHTML = staffDocuments();
      return;
    }
    if (page === 'appointments') {
      root.innerHTML = staffAppointments();
      return;
    }
    if (page === 'chat') {
      renderStaffChat(root);
      return;
    }
    if (page === 'reports') {
      root.innerHTML = staffReports();
      return;
    }
  } else {
    if (page === 'dashboard') {
      root.innerHTML = studentDashboard();
      return;
    }
    if (page === 'visa') {
      root.innerHTML = studentVisa();
      return;
    }
    if (page === 'documents') {
      root.innerHTML = studentDocuments();
      return;
    }
    if (page === 'appointments') {
      root.innerHTML = studentAppointments();
      return;
    }
  }
}

function staffDashboard() {
  return '<h1>Staff Dashboard</h1><p class="sub">Staff screens will be implemented after student screens are finalized.</p>';
}

function staffStudents() {
  return '<h1>Student Records</h1><p class="sub">Staff screens will be implemented after student screens are finalized.</p>';
}

function staffVisa() {
  return '<h1>Visa & Medical Processing</h1><p class="sub">Staff screens will be implemented after student screens are finalized.</p>';
}

function staffDocuments() {
  return '<h1>Document Verification</h1><p class="sub">Staff screens will be implemented after student screens are finalized.</p>';
}

function staffAppointments() {
  return '<h1>Appointments Management</h1><p class="sub">Staff screens will be implemented after student screens are finalized.</p>';
}

function renderStaffChat(root) {
  root.innerHTML = '<h1>Live Chat</h1><p class="sub">Staff screens will be implemented after student screens are finalized.</p>';
}

function staffReports() {
  return '<h1>Reports & Analytics</h1><p class="sub">Staff screens will be implemented after student screens are finalized.</p>';
}

function studentDashboard() {
  return `
    <h1 class="student-welcome">Welcome, Ahmed A.</h1>
    <div class="student-dashboard-grid">
      <article class="student-card">
        <header><h3>Visa Application Status</h3>${renderIcon('visa', 'card-icon')}</header>
        <div class="student-line"><p class="student-label">EMGS Review</p><span class="student-pill amber">In Progress</span></div>
        <div class="student-progress"><span style="width:60%"></span></div>
        <p class="student-muted">60% Complete</p>
      </article>

      <article class="student-card">
        <header><h3>Medical Check</h3><span class="student-green-icon">${renderIcon('medical', 'card-icon')}</span></header>
        <div class="student-line"><p class="student-label">Health Screening</p><span class="student-pill green">Completed</span></div>
        <p class="student-meta">Completed on 28 Oct 2025</p>
        <button class="student-secondary">View Report</button>
      </article>

      <article class="student-card">
        <header><h3>Next Appointment</h3>${renderIcon('appointments', 'card-icon')}</header>
        <p>Document Verification</p>
        <p class="student-meta">5 Nov 2025, 10:00 AM</p>
        <button class="student-primary">View Details</button>
      </article>
    </div>

    <section class="student-card student-wide-card">
      <h3>Visa Process Timeline</h3>
      <p class="student-muted">Track your visa application progress</p>
      <div class="student-timeline">
        <div class="node done"><span>✓</span><label>Submitted</label></div>
        <div class="line active"></div>
        <div class="node active"><span>2</span><label>EMGS Review</label></div>
        <div class="line"></div>
        <div class="node"><span>3</span><label>Immigration</label></div>
        <div class="line"></div>
        <div class="node"><span>4</span><label>Approved</label></div>
      </div>
    </section>

    <section class="student-card student-wide-card">
      <h3>Recent Updates</h3>
      <p class="student-muted">Latest notifications and updates</p>
      <div class="student-update-row">
        <span class="student-update-check">✓</span>
        <div>
          <strong>Passport Document Approved</strong>
          <p class="student-muted">2 hours ago</p>
        </div>
      </div>
    </section>
  `;
}

function studentVisa() {
  return '<h1 class="student-welcome">Visa & Medical Status</h1><section class="student-card student-wide-card"><p class="student-muted">Student page under development.</p></section>';
}

function studentDocuments() {
  return '<h1 class="student-welcome">Documents</h1><section class="student-card student-wide-card"><p class="student-muted">Student page under development.</p></section>';
}

function studentAppointments() {
  return '<h1 class="student-welcome">Appointments</h1><section class="student-card student-wide-card"><p class="student-muted">Student page under development.</p></section>';
}

setRole('student');
showScreen('landing');

window.addEventListener('click', (event) => {
  if (event.target.matches('[data-action]')) {
    event.preventDefault();
    openModal('Demo Action', `Triggered: ${event.target.dataset.action}`);
  }
});
