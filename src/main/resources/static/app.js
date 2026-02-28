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
    otp: '123456',
    currentPage: 'dashboard',
    currentChat: 'Ahmed A.'
  };
  
  const staffMenu = [
    ['dashboard', 'Dashboard'],
    ['students', 'Student Records'],
    ['visa', 'Visa & Medical Processing'],
    ['documents', 'Document Verification'],
    ['appointments', 'Appointments Management'],
    ['chat', 'Chatbot & Live chat'],
    ['reports', 'Reports & Analytics']
  ];
  const studentMenu = [
    ['dashboard', 'Dashboard'],
    ['visa', 'Visa & Medical Status'],
    ['documents', 'Documents'],
    ['appointments', 'Appointments']
  ];
  
  function showScreen(name) {
    Object.values(screens).forEach(s => s.classList.remove('active'));
    screens[name].classList.add('active');
  }
  
  function setRole(role) {
    state.role = role;
    document.getElementById('tab-student').classList.toggle('active', role === 'student');
    document.getElementById('tab-staff').classList.toggle('active', role === 'staff');
    document.getElementById('login-btn').textContent = role === 'student' ? 'Login as Student' : 'Login as Staff';
    document.getElementById('studentId').value = '';
    document.getElementById('password').value = '';
    document.getElementById('studentId').placeholder = role === 'student' ? 'Enter your student ID' : 'Enter your staff ID';
  }
  
  function openModal(title, body) {
    modalTitle.textContent = title;
    modalBody.textContent = body;
    modal.classList.remove('hidden');
  }
  
  document.getElementById('modal-cancel').onclick = () => modal.classList.add('hidden');
  document.getElementById('modal-ok').onclick = () => modal.classList.add('hidden');
  document.getElementById('go-login').onclick = e => { e.preventDefault(); showScreen('login'); };
  
  document.querySelectorAll('[data-role]').forEach(btn => btn.onclick = () => {
    setRole(btn.dataset.role);
    showScreen('login');
  });
  
  document.getElementById('tab-student').onclick = () => setRole('student');
  document.getElementById('tab-staff').onclick = () => setRole('staff');
  
  document.getElementById('login-btn').onclick = () => {
    const id = document.getElementById('studentId').value.trim();
    const pass = document.getElementById('password').value.trim();
    const err = document.getElementById('login-error');
    if (!id || !pass) {
      err.textContent = 'Please enter ID and password';
      return;
    }
    if (pass.length < 6) {
      err.textContent = 'Password must be at least 6 characters';
      return;
    }
    err.textContent = '';
    showScreen('otp');
  };
  
  document.getElementById('verify-btn').onclick = () => {
    const code = [...document.querySelectorAll('.otp-inputs input')].map(i => i.value).join('');
    const err = document.getElementById('otp-error');
    if (code.length !== 6) {
      err.textContent = '请输入完整的 6 位验证码';
      return;
    }
    if (code !== state.otp) {
      err.textContent = '验证码错误（演示码：123456）';
      return;
    }
    err.textContent = '';
    openPortal();
  };
  
  document.getElementById('resend').onclick = e => {
    e.preventDefault();
    openModal('Resend OTP', '演示环境：验证码已重新发送，仍为 123456。');
  };
  
  function openPortal() {
    showScreen('portal');
    state.currentPage = 'dashboard';
    renderShell();
    renderPage();
  }
  
  function renderShell() {
    const menuRoot = document.getElementById('menu');
    const isStaff = state.role === 'staff';
    const menu = isStaff ? staffMenu : studentMenu;
  
    document.getElementById('portal-type').textContent = isStaff ? 'Staff Portal' : 'Student Portal';
    document.getElementById('portal-title').textContent = isStaff ? 'International Office Portal' : 'Student Portal';
    document.getElementById('portal-subtitle').textContent = isStaff ? 'Administrative Dashboard' : 'Unified E-Services';
    document.getElementById('user-name').textContent = isStaff ? 'Officer Lim' : 'Ahmed A.';
    document.getElementById('user-role').textContent = isStaff ? 'Administrator' : 'Student';
  
    menuRoot.innerHTML = menu.map(([key, label]) =>
      `<button class="menu-item ${state.currentPage === key ? 'active' : ''}" data-page="${key}">${label}</button>`
    ).join('');
    menuRoot.querySelectorAll('.menu-item').forEach(el => {
      el.onclick = () => {
        state.currentPage = el.dataset.page;
        renderShell();
        renderPage();
      };
    });
  }
  
  function renderPage() {
    const root = document.getElementById('page-content');
    const isStaff = state.role === 'staff';
    const p = state.currentPage;
    if (isStaff) {
      if (p === 'dashboard') return root.innerHTML = staffDashboard();
      if (p === 'students') return root.innerHTML = staffStudents();
      if (p === 'visa') return root.innerHTML = staffVisa();
      if (p === 'documents') return root.innerHTML = staffDocuments();
      if (p === 'appointments') return root.innerHTML = staffAppointments();
      if (p === 'chat') return renderStaffChat(root);
      if (p === 'reports') return root.innerHTML = staffReports();
    } else {
      if (p === 'dashboard') return root.innerHTML = studentDashboard();
      if (p === 'visa') return root.innerHTML = studentVisa();
      if (p === 'documents') return root.innerHTML = studentDocuments();
      if (p === 'appointments') return root.innerHTML = studentAppointments();
    }
  
    bindCommonActions();
  }
  
  function bindCommonActions() {
    document.querySelectorAll('[data-action]').forEach(btn => {
      btn.onclick = () => openModal('交互演示', `你点击了 ${btn.dataset.action}。下一步可接后端 API。`);
    });
  }
  
  function staffDashboard() {
    return `
    <h1>Dashboard Overview</h1><p class="sub">Welcome back! Here's what's happening with your international students</p>
    <div class="cards">
      <div class="card"><div>New Visa Requests Today</div><div class="stat">8</div><small class="muted">Updated today</small></div>
      <div class="card"><div>Pending Verifications</div><div class="stat">23</div><small class="muted">Updated today</small></div>
      <div class="card"><div>Upcoming Appointments</div><div class="stat">15</div><small class="muted">Updated today</small></div>
      <div class="card"><div>Approved Today</div><div class="stat">12</div><small class="muted">Updated today</small></div>
    </div>
    <div class="layout-2">
      <div class="card"><h3>Visa Applications by Status</h3><p class="muted">Pie chart area (demo)</p></div>
      <div class="card"><h3>Average Processing Time</h3><p class="muted">Line chart area (demo)</p></div>
    </div>`;
  }
  
  function staffStudents() {
    return `<h1>Student Records</h1><p class="sub">Manage all international student information</p>
    <div class="card"><input placeholder="Search by name or student ID..."/></div>
    <div class="card" style="margin-top:16px"><table class="table"><thead><tr>
    <th>Student ID</th><th>Name</th><th>Country</th><th>Course</th><th>Status</th><th>Visa Status</th><th>Actions</th></tr></thead>
    <tbody>
    <tr><td>ST123456</td><td>Ahmed A.</td><td>Malaysia</td><td>CS</td><td><span class="badge b-success">ACTIVE</span></td><td><span class="badge b-info">IN-PROGRESS</span></td><td class="actions"><button data-action="View Student">View</button><button data-action="Edit Student">Edit</button></td></tr>
    <tr><td>ST123457</td><td>Li Wei</td><td>China</td><td>BA</td><td><span class="badge b-success">ACTIVE</span></td><td><span class="badge b-success">APPROVED</span></td><td class="actions"><button data-action="View Student">View</button><button data-action="Edit Student">Edit</button></td></tr>
    </tbody></table></div>`;
  }
  
  function staffVisa() {
    return `<h1>Visa & Medical Processing</h1><p class="sub">Update and manage student visa application stages</p>
    <div class="cards">
      <div class="card"><div>In Progress</div><div class="stat">18</div></div>
      <div class="card"><div>Pending Review</div><div class="stat">12</div></div>
      <div class="card"><div>Approved Today</div><div class="stat">5</div></div>
      <div class="card"><div>Avg Processing Time</div><div class="stat">8.5</div></div>
    </div>
    <div class="card" style="margin-top:16px"><table class="table"><thead><tr><th>Student</th><th>Current Stage</th><th>Progress</th><th>Status</th><th>Actions</th></tr></thead>
    <tbody>
    <tr><td>Ahmed A.</td><td>EMGS Review</td><td><div class="progress"><span style="width:40%"></span></div></td><td><span class="badge b-info">IN PROGRESS</span></td><td><button data-action="Update Stage">Update Stage</button></td></tr>
    <tr><td>Li Wei</td><td>Visa Approved</td><td><div class="progress"><span style="width:100%"></span></div></td><td><span class="badge b-success">COMPLETED</span></td><td><button data-action="Update Stage">Update Stage</button></td></tr>
    </tbody></table></div>`;
  }
  
  function staffDocuments() {
    return `<h1>Document Verification</h1><p class="sub">Review and approve student documents</p>
    <div class="cards"><div class="card"><div>Pending Review</div><div class="stat">2</div></div><div class="card"><div>Approved</div><div class="stat">2</div></div><div class="card"><div>Rejected</div><div class="stat">1</div></div></div>
    <div class="card" style="margin-top:16px"><table class="table"><thead><tr><th>Student</th><th>Document</th><th>Status</th><th>Actions</th></tr></thead><tbody>
    <tr><td>Ahmed A.</td><td>Passport Copy</td><td><span class="badge b-warning">PENDING</span></td><td class="actions"><button data-action="Preview">Preview</button><button data-action="Approve">Approve</button><button data-action="Reject">Reject</button></td></tr>
    <tr><td>Ahmed A.</td><td>Offer Letter</td><td><span class="badge b-warning">PENDING</span></td><td class="actions"><button data-action="Preview">Preview</button><button data-action="Approve">Approve</button><button data-action="Reject">Reject</button></td></tr>
    </tbody></table></div>`;
  }
  
  function staffAppointments() {
    return `<h1>Appointments Management</h1><p class="sub">Manage student appointments and consultations</p>
    <div class="cards"><div class="card"><div>Today's Appointments</div><div class="stat">2</div></div><div class="card"><div>Pending Requests</div><div class="stat">1</div></div><div class="card"><div>This Week</div><div class="stat">4</div></div><div class="card"><div>Completed</div><div class="stat">2</div></div></div>
    <div class="layout-2"><div class="card"><h3>Calendar</h3><p class="muted">Calendar placeholder</p></div><div class="card"><h3>Appointments</h3><div class="seg"><button class="active">All</button><button>Pending</button><button>Completed</button></div><div style="margin-top:12px;border:1px solid var(--line);padding:12px;border-radius:12px"><b>Ahmed A.</b><p class="muted">5 Nov 2025 · 10:00 AM</p><div class="actions"><button data-action="Reschedule">Reschedule</button><button data-action="Cancel">Cancel</button></div></div></div></div>`;
  }
  
  function renderStaffChat(root) {
    root.innerHTML = `<h1>Live Chat</h1><p class="sub">Manage student conversations in real-time</p>
    <div class="cards"><div class="card"><div>Active Chats</div><div class="stat">1</div></div><div class="card"><div>Waiting</div><div class="stat" style="color:var(--warning)">2</div></div><div class="card"><div>Unread Messages</div><div class="stat" style="color:var(--danger)">3</div></div><div class="card"><div>Avg Response Time</div><div class="stat" style="color:var(--success)">2.3m</div></div></div>
    <div class="chat-wrap" style="margin-top:16px">
      <div class="chat-list">
        <div class="chat-list-item" data-chat="Ahmed A."><b>Ahmed A.</b><div class="muted">Thank you for the update!</div><span class="badge b-success">ACTIVE</span></div>
        <div class="chat-list-item" data-chat="Li Wei"><b>Li Wei</b><div class="muted">When will my documents be verified?</div><span class="badge b-warning">WAITING</span></div>
        <div class="chat-list-item" data-chat="Sarah K."><b>Sarah K.</b><div class="muted">I need help with my visa</div><span class="badge b-warning">WAITING</span></div>
      </div>
      <div class="chat-panel">
        <div style="padding:12px;border-bottom:1px solid var(--line)"><b id="chat-title">${state.currentChat}</b></div>
        <div id="chat-msgs" style="padding:14px; min-height:360px">
          <div class="msg me">Hello! How can I help you today?</div>
          <div class="msg other">Hi! I have a question about my visa application status.</div>
          <div class="msg me">Of course! Can you provide your student ID?</div>
        </div>
        <div class="chat-input"><input id="chat-input" placeholder="Type your message..."/><button id="send-chat" class="btn btn-primary">Send</button></div>
      </div>
    </div>`;
  
    root.querySelectorAll('[data-chat]').forEach(item => item.onclick = () => {
      state.currentChat = item.dataset.chat;
      root.querySelector('#chat-title').textContent = state.currentChat;
    });
    root.querySelector('#send-chat').onclick = () => {
      const input = root.querySelector('#chat-input');
      const text = input.value.trim();
      if (!text) return;
      const box = root.querySelector('#chat-msgs');
      const div = document.createElement('div');
      div.className = 'msg me';
      div.textContent = text;
      box.appendChild(div);
      input.value = '';
    };
  }
  
  function staffReports() {
    return `<h1>Reports & Analytics</h1><p class="sub">View insights and export data reports</p>
    <div class="cards"><div class="card"><div>Total Applications</div><div class="stat">353</div></div><div class="card"><div>This Month</div><div class="stat">42</div></div><div class="card"><div>Approval Rate</div><div class="stat">94%</div></div><div class="card"><div>Avg Processing Time</div><div class="stat">8.8</div></div></div>
    <div class="layout-2"><div class="card"><h3>Applications per Month</h3><p class="muted">Line chart placeholder</p></div><div class="card"><h3>Visa Status Breakdown</h3><p class="muted">Pie chart placeholder</p></div></div>`;
  }
  
  function studentDashboard() {
    return `<h1>Welcome, Ahmed A.</h1><p class="sub">Here's an overview of your international student services</p>
    <div class="cards">
      <div class="card"><div>Visa Application Status</div><div style="margin-top:10px"><span class="badge b-warning">In Progress</span></div><p class="muted">60% Complete</p></div>
      <div class="card"><div>Medical Check</div><div style="margin-top:10px"><span class="badge b-success">Completed</span></div><button data-action="View Report">View Report</button></div>
      <div class="card"><div>Next Appointment</div><p class="muted">5 Nov 2025 · 10:00 AM</p><button class="btn btn-primary" data-action="View Details">View Details</button></div>
      <div class="card"><div>Support Chat</div><p class="muted">1 unread message</p><button data-action="Open Chat">Open</button></div>
    </div>
    <div class="card" style="margin-top:16px"><h3>Visa Process Timeline</h3><p class="muted">Submitted → EMGS Review → Immigration → Approved</p></div>`;
  }
  
  function studentVisa() {
    return `<h1>Visa & Medical Status</h1><p class="sub">Track your visa application and medical examination progress</p>
    <div class="seg"><button class="active">Visa Process Timeline</button><button>Medical Report Status</button></div>
    <div class="card" style="margin-top:16px"><table class="table"><thead><tr><th>Stage</th><th>Date</th><th>Officer</th><th>Status</th></tr></thead><tbody>
    <tr><td>Application Submitted</td><td>15 Oct 2025</td><td>System</td><td><span class="badge b-success">COMPLETED</span></td></tr>
    <tr><td>EMGS Processing</td><td>20 Oct 2025</td><td>Officer Lim</td><td><span class="badge b-warning">IN PROGRESS</span></td></tr>
    <tr><td>Immigration Approval</td><td>Pending</td><td>-</td><td><span class="badge b-gray">PENDING</span></td></tr>
    </tbody></table></div>`;
  }
  
  function studentDocuments() {
    return `<h1>Documents</h1><p class="sub">Upload and manage your required documents</p>
    <div class="card"><h3>Upload Documents</h3><p class="muted">PDF, JPG, PNG</p><input type="file"/><button class="btn btn-primary" data-action="Upload Files">Upload</button></div>
    <div class="card" style="margin-top:16px"><h3>Uploaded Documents</h3><table class="table"><thead><tr><th>Document</th><th>Status</th><th>Date</th><th>Remarks</th><th>Action</th></tr></thead><tbody>
    <tr><td>Passport Copy</td><td><span class="badge b-success">APPROVED</span></td><td>30 Oct 2025</td><td>Verified</td><td><button data-action="Download">Download</button></td></tr>
    <tr><td>Offer Letter</td><td><span class="badge b-warning">PENDING</span></td><td>1 Nov 2025</td><td>Awaiting Review</td><td><button data-action="Download">Download</button></td></tr>
    </tbody></table></div>`;
  }
  
  function studentAppointments() {
    return `<h1>Appointments</h1><p class="sub">Manage upcoming and past appointments</p>
    <div class="card"><h3>Upcoming Appointments</h3>
    <div style="border:1px solid var(--line);padding:10px;border-radius:12px;margin-bottom:10px"><b>Document Verification</b><p class="muted">5 Nov 2025 · 10:00 AM · International Office Block A</p><span class="badge b-success">CONFIRMED</span><div class="actions"><button data-action="Reschedule">Reschedule</button><button data-action="Cancel">Cancel</button></div></div>
    <div style="border:1px solid var(--line);padding:10px;border-radius:12px"><b>Visa Consultation</b><p class="muted">8 Nov 2025 · 2:00 PM · International Office Block A</p><span class="badge b-success">CONFIRMED</span><div class="actions"><button data-action="Reschedule">Reschedule</button><button data-action="Cancel">Cancel</button></div></div></div>
    <div class="card" style="margin-top:16px"><h3>Past Appointments</h3><table class="table"><tbody><tr><td>Medical Check-up</td><td>28 Oct 2025</td><td><span class="badge b-gray">COMPLETED</span></td></tr><tr><td>Initial Registration</td><td>15 Oct 2025</td><td><span class="badge b-gray">COMPLETED</span></td></tr></tbody></table></div>`;
  }
  
  // initial bindings
  setRole('student');
  showScreen('landing');
  
  window.addEventListener('click', (e) => {
    if (e.target.matches('[data-action]')) {
      e.preventDefault();
      openModal('交互演示', `已触发：${e.target.dataset.action}`);
    }
  });
  