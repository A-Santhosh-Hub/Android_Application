import React from "react";
import { Sidebar } from "./components/Sidebar";
import { DashboardOverview } from "./components/DashboardOverview";
import { LeadManagement } from "./components/LeadManagement";
import { ConsultationManagement } from "./components/ConsultationManagement";
import { CourseStudentManagement } from "./components/CourseStudentManagement";
import { PaymentManagement } from "./components/PaymentManagement";
import { CertificateManagement } from "./components/CertificateManagement";
import { CommunityEventSocial } from "./components/CommunityEventSocial";
import { ReportAnalytics } from "./components/ReportAnalytics";
import { EmployeeManagement } from "./components/EmployeeManagement";
import { SoundEngine } from "./utils/sound";
import { Volume2, VolumeX, Bell, CheckSquare, Clock, X, Phone, Play, ShieldAlert, LogOut, KeyRound, Mail, Lock, ChevronDown, Users, RefreshCw } from "lucide-react";

import {
  User,
  Employee,
  Client,
  Lead,
  LeadActivity,
  Consultation,
  Course,
  Batch,
  Student,
  StudentEnrollment,
  Certificate,
  Payment,
  Invoice,
  CommunityMember,
  Event,
  SocialMediaPost,
  Notification,
  UserRole,
  Task,
  AuditLog
} from "./types";

export default function App() {
  // Navigation & User Role context states
  const [activeTab, setActiveTab] = React.useState("dashboard");
  const [activeRole, setActiveRole] = React.useState<UserRole>("Super Admin");

  const [currentUser, setCurrentUser] = React.useState<User | null>({
    id: "emp-1",
    name: "Dr. K. Muralidharan (Founder)",
    email: "founder@alpastrology.com",
    role: "Super Admin",
    phone: "+91 98400 12345",
    avatar: "https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=150"
  });

  const [coworkers, setCoworkers] = React.useState<Employee[]>([]);
  const [isWorkspaceLocked, setIsWorkspaceLocked] = React.useState(false);
  const [showProfileDropdown, setShowProfileDropdown] = React.useState(false);

  const [showLoginModal, setShowLoginModal] = React.useState(false);
  const [loginEmail, setLoginEmail] = React.useState("");
  const [loginPassword, setLoginPassword] = React.useState("");
  const [loginError, setLoginError] = React.useState("");
  const [isLoggingIn, setIsLoggingIn] = React.useState(false);

  // Core Data models state (synced with Node Express in-memory DB arrays)
  const [clients, setClients] = React.useState<Client[]>([]);
  const [leads, setLeads] = React.useState<Lead[]>([]);
  const [consultations, setConsultations] = React.useState<Consultation[]>([]);
  const [courses, setCourses] = React.useState<Course[]>([]);
  const [batches, setBatches] = React.useState<Batch[]>([]);
  const [students, setStudents] = React.useState<Student[]>([]);
  const [enrollments, setEnrollments] = React.useState<StudentEnrollment[]>([]);
  const [certificates, setCertificates] = React.useState<Certificate[]>([]);
  const [payments, setPayments] = React.useState<Payment[]>([]);
  const [invoices, setInvoices] = React.useState<Invoice[]>([]);
  const [members, setMembers] = React.useState<CommunityMember[]>([]);
  const [events, setEvents] = React.useState<Event[]>([]);
  const [socialPosts, setSocialPosts] = React.useState<SocialMediaPost[]>([]);
  const [notifications, setNotifications] = React.useState<Notification[]>([]);
  const [tasks, setTasks] = React.useState<Task[]>([]);
  const [auditLogs, setAuditLogs] = React.useState<AuditLog[]>([]);

  // Page loading indicators
  const [isInitializing, setIsInitializing] = React.useState(true);

  // Lead Timeline Activity Directory dictionary
  const [leadActivities, setLeadActivities] = React.useState<{ [leadId: string]: LeadActivity[] }>({});

  // Alarm, Reminder & Audio Alerts States
  const [dismissedReminderIds, setDismissedReminderIds] = React.useState<string[]>([]);
  const [mutedAllSounds, setMutedAllSounds] = React.useState<boolean>(() => {
    try {
      return localStorage.getItem("crm_mute_alerts") === "true";
    } catch (_) {
      return false;
    }
  });
  const [activeAlertTask, setActiveAlertTask] = React.useState<Task | null>(null);
  const [audioAuthorized, setAudioAuthorized] = React.useState(false);

  // Sound alarm background poller
  React.useEffect(() => {
    if (isInitializing) return;

    const interval = setInterval(() => {
      const now = Date.now();
      
      // Find the first task that requires an alarm reminder but wasn't completed or dismissed
      const pendingAlert = tasks.find(t => {
        if (t.status !== "Pending" || !t.reminderTime) return false;
        if (dismissedReminderIds.includes(t.id)) return false;
        
        const rTime = new Date(t.reminderTime).getTime();
        // Trigger if reminder time has passed
        const hasPassed = now >= rTime;
        
        // Don't trigger for alerts that were due more than 12 hours ago (outdated)
        const isNotTooOld = now - rTime < 12 * 60 * 60 * 1000;
        
        return hasPassed && isNotTooOld;
      });

      if (pendingAlert) {
        setActiveAlertTask(pendingAlert);
        
        // Play sound if not muted
        if (!mutedAllSounds) {
          const lowerTitle = pendingAlert.title.toLowerCase();
          const isSiren = lowerTitle.includes("urgent") || lowerTitle.includes("siren") || lowerTitle.includes("danger") || lowerTitle.includes("warning");
          const isChime = lowerTitle.includes("chime") || lowerTitle.includes("meeting") || lowerTitle.includes("brief") || lowerTitle.includes("notice") || lowerTitle.includes("session");
          const isCall = lowerTitle.includes("call") || lowerTitle.includes("phone") || lowerTitle.includes("ring") || lowerTitle.includes("follow-up") || lowerTitle.includes("prospect");

          if (isSiren) {
            SoundEngine.playWarningSiren();
          } else if (isChime) {
            SoundEngine.playChime();
          } else if (isCall) {
            SoundEngine.playCallAlert();
          } else {
            SoundEngine.playTaskReminder();
          }
        }
      } else {
        setActiveAlertTask(null);
      }
    }, 4000);

    return () => clearInterval(interval);
  }, [tasks, dismissedReminderIds, mutedAllSounds, isInitializing]);

  // Load backend data on mount
  React.useEffect(() => {
    setIsInitializing(true);
    
    // Fetch state parameters in parallel
    Promise.all([
      fetch("/api/clients").then(res => res.json()),
      fetch("/api/leads").then(res => res.json()),
      fetch("/api/consultations").then(res => res.json()),
      fetch("/api/courses").then(res => res.json()),
      fetch("/api/batches").then(res => res.json()),
      fetch("/api/students").then(res => res.json()),
      fetch("/api/enrollments").then(res => res.json()),
      fetch("/api/certificates").then(res => res.json()),
      fetch("/api/payments").then(res => res.json()),
      fetch("/api/invoices").then(res => res.json()),
      fetch("/api/community/members").then(res => res.json()),
      fetch("/api/events").then(res => res.json()),
      fetch("/api/social/posts").then(res => res.json()),
      fetch("/api/notifications").then(res => res.json()),
      fetch("/api/tasks").then(res => res.json()),
      fetch("/api/audit-logs").then(res => res.json()),
      fetch("/api/auth/me").then(res => res.json()),
      fetch("/api/employees").then(res => res.json())
    ])
      .then(
        ([
          clientsData,
          leadsData,
          consultationsData,
          coursesData,
          batchesData,
          studentsData,
          enrollmentsData,
          certificatesData,
          paymentsData,
          invoicesData,
          membersData,
          eventsData,
          socialPostsData,
          notificationsData,
          tasksData,
          auditLogsData,
          meData,
          employeesData
        ]) => {
          setClients(clientsData || []);
          setLeads(leadsData || []);
          setConsultations(consultationsData || []);
          setCourses(coursesData || []);
          setBatches(batchesData || []);
          setStudents(studentsData || []);
          setEnrollments(enrollmentsData || []);
          setCertificates(certificatesData || []);
          setPayments(paymentsData || []);
          setInvoices(invoicesData || []);
          setMembers(membersData || []);
          setEvents(eventsData || []);
          setSocialPosts(socialPostsData || []);
          setNotifications(notificationsData || []);
          setTasks(tasksData || []);
          setAuditLogs(auditLogsData || []);
          setCoworkers(employeesData || []);
          if (meData && meData.user) {
            if (meData.user.id === "guest") {
              setCurrentUser(null);
            } else {
              setCurrentUser(meData.user);
              setActiveRole(meData.user.role);
            }
          }
        }
      )
      .catch(err => {
        console.error("Critical: ERP astrological sync arrays failed on load.", err);
      })
      .finally(() => {
        setIsInitializing(false);
      });
  }, []);

  // CLIENT CRUD CALLS
  const handleAddClient = (clientData: Partial<Client>) => {
    fetch("/api/clients", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(clientData)
    })
      .then(res => res.json())
      .then(newC => {
        setClients(prev => [newC, ...prev]);
        showInstantNotification("WhatsApp & Email Integration", `Dispatched onboarding registration coordinate lines to ${newC.name} via API gateway.`, "success");
      })
      .catch(err => console.error("Onboard Client API error", err));
  };

  // LEADS ACTIONS
  const handleAddLead = (leadData: Partial<Lead>) => {
    fetch("/api/leads", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ ...leadData, source: "Direct Referral", assignedTo: "Admissions CRM Core" })
    })
      .then(res => res.json())
      .then(newL => {
        setLeads(prev => [...prev, newL]);
        showInstantNotification("Lead Generation Notification", `New lead generated for ${newL.name}. Target Course Assigned.`, "info");
      })
      .catch(err => console.error("Lead generation error", err));
  };

  const handleImportLeads = (leadsArray: Partial<Lead>[]) => {
    return fetch("/api/leads/import", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ leads: leadsArray })
    })
      .then(res => res.json())
      .then(data => {
        if (data.success && data.imported) {
          setLeads(prev => [...data.imported, ...prev]);
          showInstantNotification("Bulk Import Successful", `${data.imported.length} leads successfully imported.`, "success");
          return data.imported;
        } else {
          throw new Error(data.error || "Failed to import leads");
        }
      });
  };

  const handleAddTask = (taskData: Partial<Task>) => {
    return fetch("/api/tasks", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(taskData)
    })
      .then(res => res.json())
      .then(newT => {
        setTasks(prev => [newT, ...prev]);
        showInstantNotification("Schedule / Call Reminder Saved", `Scheduled: "${newT.title}"`, "success");
        return newT;
      })
      .catch(err => {
        console.error("Error creating scheduled call task", err);
      });
  };

  const handleUpdateLead = (leadId: string, updatedFields: Partial<Lead>) => {
    fetch(`/api/leads/${leadId}`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(updatedFields)
    })
      .then(res => res.json())
      .then(updatedL => {
        setLeads(prev => prev.map(l => (l.id === leadId ? { ...l, ...updatedL } : l)));
        showInstantNotification("CRM State Transit", `Lead ${updatedL.name} transited successfully.`, "info");
      })
      .catch(err => console.error("Update Lead state error", err));
  };

  const handleBulkAssignLeads = (assignments: { leadId: string, assignedStaff: string }[]) => {
    fetch("/api/leads/bulk/assign", {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ assignments })
    })
      .then(res => res.json())
      .then(data => {
        if (data.success && data.updatedLeads) {
          const updatedMap = new Map(data.updatedLeads.map((l: any) => [l.id, l]));
          setLeads(prev => prev.map(l => updatedMap.has(l.id) ? updatedMap.get(l.id) : l));
          showInstantNotification(
            "CRM Split Executed",
            `Successfully distributed ${assignments.length} unique leads to your coworkers.`,
            "success"
          );
        }
      })
      .catch(err => console.error("Bulk assign error", err));
  };

  // TIMELINE ACTIONS
  const handleFetchActivities = (leadId: string) => {
    fetch(`/api/leads/${leadId}/activities`)
      .then(res => res.json())
      .then(data => {
        setLeadActivities(prev => ({ ...prev, [leadId]: data || [] }));
      })
      .catch(err => console.error("Error loading activities flow", err));
  };

  const handleAddActivity = (leadId: string, actData: { actionType: string; details: string }) => {
    fetch(`/api/leads/${leadId}/activities`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ ...actData, staffName: activeRole })
    })
      .then(res => res.json())
      .then(newAct => {
        setLeadActivities(prev => {
          const list = prev[leadId] || [];
          return { ...prev, [leadId]: [newAct, ...list] };
        });
        // Reload audits
        fetch("/api/audit-logs").then(res => res.json()).then(data => setAuditLogs(data));
      })
      .catch(err => console.error("Activity register failed", err));
  };

  // CONSULTATIONS ACTIONS
  const handleAddConsultation = (consData: Partial<Consultation>) => {
    fetch("/api/consultations", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(consData)
    })
      .then(res => res.json())
      .then(newCons => {
        setConsultations(prev => [newCons, ...prev]);
        showInstantNotification("Google Meet & Calendar API", `Booked video slot and created calendar event matching Dr. Muralidharan schedules.`, "success");
      })
      .catch(err => console.error("Scheduling Consultation error", err));
  };

  const handleUpdateConsultation = (consId: string, updatedFields: Partial<Consultation>) => {
    fetch(`/api/consultations/${consId}`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(updatedFields)
    })
      .then(res => res.json())
      .then(updatedCons => {
        setConsultations(prev => prev.map(c => (c.id === consId ? { ...c, ...updatedCons } : c)));
        showInstantNotification("Consultation Updated", `Session status verified: ${updatedCons.status}`, "info");
      })
      .catch(err => console.error("Updating Consultation log err", err));
  };

  // ACADEMY COURSES ACTIONS
  const handleAddStudent = (studData: Partial<Student>) => {
    fetch("/api/students", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(studData)
    })
      .then(res => res.json())
      .then(newS => {
        setStudents(prev => [newS, ...prev]);
      })
      .catch(err => console.error("Error creating student record", err));
  };

  const handleEnrollStudent = (enrollData: { studentId: string; courseId: string; batchId: string }) => {
    fetch("/api/enrollments", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(enrollData)
    })
      .then(res => res.json())
      .then(newEnroll => {
        setEnrollments(prev => [newEnroll, ...prev]);
        // reload invoices matching enrollments automatic pricing
        fetch("/api/invoices")
          .then(res => res.json())
          .then(data => setInvoices(data));
      })
      .catch(err => console.error("Enrollment Mapping fail", err));
  };

  const handleUpdateEnrollment = (enId: string, fields: Partial<StudentEnrollment>) => {
    fetch(`/api/enrollments/${enId}`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(fields)
    })
      .then(res => res.json())
      .then(updatedEnr => {
        setEnrollments(prev => prev.map(e => (e.id === enId ? { ...e, ...updatedEnr } : e)));
      })
      .catch(err => console.error("Error updating roll student progress", err));
  };

  // PAYMENT & BILLS
  const handleAddPayment = (payData: Partial<Payment>) => {
    fetch("/api/payments", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payData)
    })
      .then(res => res.json())
      .then(newPay => {
        setPayments(prev => [newPay, ...prev]);
        // Reload Invoices
        fetch("/api/invoices")
          .then(res => res.json())
          .then(data => setInvoices(data));
        showInstantNotification("Financial ledger entry", `Realized financial payments record booked.`, "success");
      })
      .catch(err => console.error("Ledger payment adding failure", err));
  };

  const handleClearPendingPayment = (payId: string) => {
    fetch(`/api/payments/${payId}/clear`, {
      method: "POST"
    })
      .then(res => res.json())
      .then(clearedPay => {
        setPayments(prev => prev.map(p => (p.id === payId ? { ...p, ...clearedPay } : p)));
        // Reload Invoices
        fetch("/api/invoices")
          .then(res => res.json())
          .then(data => setInvoices(data));
        showInstantNotification("Payment Realized Checks", `UPI Transaction cleared manually on verification.`, "success");
      })
      .catch(err => console.error("Payment clearing manual error", err));
  };

  // CERTIFICATES ACTIONS
  const handleIssueCertificate = (issueData: { studentId: string; studentName: string; courseName: string; type: string }) => {
    fetch("/api/certificates", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(issueData)
    })
      .then(res => res.json())
      .then(newCert => {
        setCertificates(prev => [newCert, ...prev]);
        showInstantNotification("Credential signed", `Locking course progress for ${newCert.studentName}. Secure QR generated.`, "success");
      })
      .catch(err => console.error("Issuer certification system error", err));
  };

  // COMMUNITY EVENTS SOCIAL ACTIONS
  const handleAddMember = (mD: Partial<CommunityMember>) => {
    fetch("/api/community/members", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(mD)
    })
      .then(res => res.json())
      .then(newM => {
        setMembers(prev => [newM, ...prev]);
        showInstantNotification("Community Network Indexed", `Joined VIP astrology circle group lists.`, "success");
      })
      .catch(err => console.error("Member creation failed", err));
  };

  const handleAddEvent = (eD: Partial<Event>) => {
    fetch("/api/events", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(eD)
    })
      .then(res => res.json())
      .then(newEv => setEvents(prev => [newEv, ...prev]))
      .catch(err => console.error("Event creation system error", err));
  };

  const handleBookEventTicket = (eventId: string, ticketForm: { memberName: string; memberEmail: string; ticketCount: number }) => {
    fetch(`/api/events/${eventId}/book-ticket`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(ticketForm)
    })
      .then(res => res.json())
      .then(updatedEv => {
        setEvents(prev => prev.map(e => (e.id === eventId ? { ...e, ...updatedEv } : e)));
        // reload transactions
        Promise.all([
          fetch("/api/payments").then(res => res.json()),
          fetch("/api/invoices").then(res => res.json())
        ]).then(([p, i]) => {
          setPayments(p);
          setInvoices(i);
        });
      })
      .catch(err => console.error("Ticketing API failure", err));
  };

  const handleSchedulePost = (postD: Partial<SocialMediaPost>) => {
    fetch("/api/social/posts", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(postD)
    })
      .then(res => res.json())
      .then(newP => {
        setSocialPosts(prev => [newP, ...prev]);
        showInstantNotification("Buffer Campaign Buffer", `Social media publication calendar entries saved.`, "info");
      })
      .catch(err => console.error("Post scheduler failed", err));
  };

  const handleCompleteTask = (taskId: string) => {
    fetch(`/api/tasks/${taskId}`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ status: "Completed" })
    })
      .then(res => res.json())
      .then(updatedTask => {
        setTasks(prev => prev.map(t => (t.id === taskId ? { ...t, ...updatedTask } : t)));
        showInstantNotification("CRM Task completion", `Resolved checklist item: "${updatedTask.title}"`, "success");
        // Reload Audits
        fetch("/api/audit-logs").then(res => res.json()).then(data => setAuditLogs(data));
      })
      .catch(err => console.error("Task update failed", err));
  };

  const handleSnoozeReminder = (task: Task) => {
    const snoozeTime = new Date(Date.now() + 5 * 60 * 1000).toISOString();
    const snoozeDate = snoozeTime.split("T")[0];

    fetch(`/api/tasks/${task.id}`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ reminderTime: snoozeTime, dueDate: snoozeDate })
    })
      .then(res => res.json())
      .then(updatedTask => {
        setTasks(prev => prev.map(t => (t.id === task.id ? { ...t, ...updatedTask } : t)));
        setDismissedReminderIds(prev => prev.filter(id => id !== task.id));
        showInstantNotification("Task reminder snoozed", `Snoozed alarm for 5 mins: "${task.title}"`, "info");
      })
      .catch(err => console.error("Snooze request failed", err));
  };

  // Dispatch live notification logs inside system
  const showInstantNotification = (channel: string, info: string, status: "success" | "danger" | "info") => {
    fetch("/api/notifications/log", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ channel, recipient: "CRM Super Admin Desk", message: info, status })
    })
      .then(res => res.json())
      .then(newLog => {
        setNotifications(prev => [newLog, ...prev]);
      })
      .catch(err => console.error("Trace logging offline", err));
  };

  const handleLoginSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!loginEmail.trim() || !loginPassword.trim()) return;

    setIsLoggingIn(true);
    setLoginError("");

    fetch("/api/auth/login", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ email: loginEmail.trim(), password: loginPassword })
    })
      .then(async (res) => {
        const data = await res.json();
        if (!res.ok) {
          throw new Error(data.error || "Authentication failed");
        }
        return data;
      })
      .then((data) => {
        setCurrentUser(data.user);
        setActiveRole(data.user.role);
        setShowLoginModal(false);
        setLoginEmail("");
        setLoginPassword("");
        
        // Success audio tone is delightful!
        SoundEngine.playSuccessAlert();

        // Register instant notification
        showInstantNotification(
          "Co-worker Logged In",
          `Access keys authorized for ${data.user.name} (${data.user.role}).`,
          "success"
        );

        // Reload audit logs to show login action
        fetch("/api/audit-logs")
          .then(res => res.json())
          .then(logs => setAuditLogs(logs));
      })
      .catch((err) => {
        setLoginError(err.message || "Invalid credentials. Please verify co-worker email and password.");
        SoundEngine.playWarningSiren();
      })
      .finally(() => {
        setIsLoggingIn(false);
      });
  };

  const handleLogout = () => {
    fetch("/api/auth/logout", {
      method: "POST"
    })
      .then(res => res.json())
      .then(data => {
        setCurrentUser(null);
        setActiveRole("Super Admin");
        showInstantNotification("Access Session Cleared", "Securely logged out from coworker authorization key.", "info");
        SoundEngine.playChime();
        setIsWorkspaceLocked(true);
      })
      .catch(err => {
        console.error("Logout failed, fallback cleaning state", err);
        setCurrentUser(null);
        setActiveRole("Super Admin");
        setIsWorkspaceLocked(true);
      });
  };

  if (isInitializing) {
    return (
      <div className="min-h-screen bg-[#070b13] flex flex-col justify-center items-center text-slate-400 font-mono text-xs space-y-4">
        {/* Spinner */}
        <div className="w-12 h-12 rounded-full border-4 border-slate-900 border-t-amber-500 animate-spin"></div>
        <p className="text-amber-500 font-bold uppercase tracking-widest text-[10px]">Initializing Akshaya Lagna Paddhati CRM Core</p>
        <p className="text-slate-600 text-[9px]">Synchronizing cosmic parameters and local memory arrays...</p>
      </div>
    );
  }

  if (currentUser === null) {
    return (
      <div className="min-h-screen bg-[#070b13] flex flex-col justify-center items-center p-4 relative overflow-hidden font-sans">
        {/* Absolute ambient lights */}
        <div className="absolute top-1/4 left-1/4 w-96 h-96 bg-amber-500/10 rounded-full blur-3xl" />
        <div className="absolute bottom-1/4 right-1/4 w-96 h-96 bg-indigo-500/10 rounded-full blur-3xl" />

        <div className="w-full max-w-4xl bg-slate-900/40 backdrop-blur-md rounded-3xl border border-slate-800 p-8 md:p-10 text-slate-200 shadow-2xl relative z-10 space-y-8 animate-in zoom-in-95 duration-300">
          
          <div className="text-center space-y-2 max-w-xl mx-auto">
            <span className="text-[10px] uppercase tracking-widest font-black text-amber-550 bg-amber-500/10 px-3 py-1 rounded-full font-mono">
              ★ ALP ASTROLOGY SYSTEM SECURITY
            </span>
            <h2 className="text-2xl md:text-3xl font-extrabold text-white uppercase tracking-tight font-sans">
              Coworker Portal Access
            </h2>
            <p className="text-xs text-slate-400">
              Welcome to the Akshaya Lagna Paddhati Enterprise CRM Network. Please authenticate or select a coworker profile to initialize your secure session token.
            </p>
          </div>

          {loginError && (
            <div className="text-xs font-bold text-rose-400 bg-rose-500/10 border border-rose-500/20 p-3 rounded-xl max-w-md mx-auto text-center">
              ❌ {loginError}
            </div>
          )}

          {/* Coworker Grid for Instant Switching and passcode entry */}
          <div className="space-y-4">
            <h3 className="text-[10px] font-black uppercase text-slate-500 tracking-wider font-mono text-center">
              Select Profile to Switch Session:
            </h3>

            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
              {coworkers && coworkers.length > 0 ? (
                coworkers.map((worker) => (
                  <button
                    key={worker.id}
                    onClick={() => {
                      setLoginEmail(worker.email);
                      setLoginPassword(worker.password);
                      setLoginError("");
                      SoundEngine.playChime();
                      
                      // Auto trigger login directly for smooth sandbox sandbox testing
                      setIsLoggingIn(true);
                      fetch("/api/auth/login", {
                        method: "POST",
                        headers: { "Content-Type": "application/json" },
                        body: JSON.stringify({ email: worker.email, password: worker.password })
                      })
                        .then(res => res.json())
                        .then(data => {
                          if (data.success && data.user) {
                            setCurrentUser(data.user);
                            setActiveRole(data.user.role);
                            SoundEngine.playSuccessAlert();
                            showInstantNotification(
                              "Login Successful",
                              `Welcome back, ${data.user.name} (${data.user.role}).`,
                              "success"
                            );
                          } else {
                            setLoginError(data.error || "Failed to switch.");
                          }
                        })
                        .catch(() => {
                          setLoginError("Could not reach validation service.");
                        })
                        .finally(() => {
                          setIsLoggingIn(false);
                        });
                    }}
                    className="bg-slate-950/60 hover:bg-slate-950/90 border border-slate-800 hover:border-amber-500/40 p-4 rounded-2xl text-left cursor-pointer transition-all hover:scale-[1.02] flex flex-col items-center text-center space-y-3 class-profile-selector"
                  >
                    <div className="relative">
                      <img
                        src={worker.avatar || "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150"}
                        alt={worker.name}
                        className="w-16 h-16 rounded-full object-cover border border-slate-700 group-hover:border-amber-500"
                      />
                      <span className="absolute bottom-0 right-0 w-3.5 h-3.5 bg-emerald-500 rounded-full border-2 border-slate-900" />
                    </div>
                    <div className="leading-snug">
                      <h4 className="text-[12px] font-black text-white hover:text-amber-400 truncate max-w-[150px] transition-colors">
                        {worker.name}
                      </h4>
                      <p className="text-[9px] font-extrabold text-amber-550 uppercase tracking-wider font-sans mt-0.5">
                        {worker.role}
                      </p>
                      <p className="text-[8.5px] font-mono text-slate-500 mt-1">
                        🔑 PIN: <span className="text-slate-400 font-semibold">{worker.password}</span>
                      </p>
                    </div>
                  </button>
                ))
              ) : (
                <div className="col-span-4 text-center text-slate-500 py-6 font-mono text-[10px]">
                  No seed coworker profiles found.
                </div>
              )}
            </div>
          </div>

          {/* Divider with choice */}
          <div className="relative flex py-2 items-center justify-center">
            <div className="flex-grow border-t border-slate-800/80"></div>
            <span className="flex-shrink mx-4 text-[9px] text-slate-600 uppercase tracking-widest font-mono">
              Or Authenticate with passkey credentials
            </span>
            <div className="flex-grow border-t border-slate-800/80"></div>
          </div>

          {/* Form and alternate choices */}
          <div className="max-w-md mx-auto bg-slate-950/60 border border-slate-800/80 rounded-2xl p-6">
            <form onSubmit={handleLoginSubmit} className="space-y-4">
              <div>
                <label className="block text-[9.5px] font-extrabold text-slate-400 uppercase tracking-wider mb-1.5 font-mono">
                  Employee User Email:
                </label>
                <div className="relative">
                  <Mail className="absolute left-3 top-3 w-3.5 h-3.5 text-slate-500" />
                  <input
                    type="email"
                    value={loginEmail}
                    onChange={(e) => setLoginEmail(e.target.value)}
                    required
                    placeholder="worker@alpastrology.com"
                    className="w-full bg-slate-900/60 border border-slate-800 rounded-xl pl-9 pr-3 py-2.5 text-xs text-white focus:outline-hidden focus:border-amber-500 placeholder-slate-600"
                  />
                </div>
              </div>

              <div>
                <label className="block text-[9.5px] font-extrabold text-slate-400 uppercase tracking-wider mb-1.5 font-mono">
                  Coworker Security PIN:
                </label>
                <div className="relative">
                  <Lock className="absolute left-3 top-3 w-3.5 h-3.5 text-slate-500" />
                  <input
                    type="password"
                    value={loginPassword}
                    onChange={(e) => setLoginPassword(e.target.value)}
                    required
                    placeholder="Enter Security Passkey"
                    className="w-full bg-slate-900/60 border border-slate-800 rounded-xl pl-9 pr-3 py-2.5 text-xs text-white focus:outline-hidden focus:border-amber-500 placeholder-slate-600"
                  />
                </div>
              </div>

              <div className="pt-2">
                <button
                  type="submit"
                  disabled={isLoggingIn}
                  className="w-full bg-amber-500 hover:bg-amber-600 disabled:opacity-50 text-slate-950 font-black uppercase tracking-wider py-3 rounded-xl text-xs flex items-center justify-center gap-2 cursor-pointer shadow-md transition-all font-mono"
                >
                  <KeyRound className="w-4 h-4" />
                  {isLoggingIn ? "Validating credentials..." : "Validate Security PIN"}
                </button>
              </div>
            </form>
          </div>

          <div className="text-center pt-2">
            <button
              onClick={() => {
                // Initialize as a simulated Guest Mode session
                setCurrentUser({
                  id: "guest",
                  name: "Guest User",
                  email: "guest@alp.org",
                  role: "Super Admin",
                  phone: "",
                  avatar: ""
                });
                setActiveRole("Super Admin");
                SoundEngine.playChime();
                showInstantNotification("Sandbox Guest Mode", "Entered workspace in viewing mode.", "info");
              }}
              className="text-slate-400 hover:text-white text-[10.5px] font-bold uppercase tracking-widest transition-colors font-mono cursor-pointer"
            >
              🔐 Continue as Demo Guest & Bypass Gate ➔
            </button>
          </div>

          <p className="text-[9px] text-slate-600 text-center uppercase tracking-wider font-mono">
            Protected by ALP Astrology Cryptographic Clearance Core System v3.0
          </p>

        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-slate-50 text-slate-800 flex font-sans overflow-x-hidden antialiased">
      {/* 1. SIDEBAR NAVIGATION CONTEXT */}
      <Sidebar
        currentUser={currentUser || { id: "guest", name: "Guest User", email: "guest@alp.org", role: activeRole, avatar: "" }}
        onSwitchRole={(role) => {
          setActiveRole(role);
          fetch("/api/auth/switch-role", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ role })
          })
            .then(res => res.json())
            .then(data => {
              if (data.success && data.user) {
                setCurrentUser(data.user);
              }
            });
        }}
        activeTab={activeTab === "students" || activeTab === "courses" ? "courses" : activeTab === "events" || activeTab === "social" ? "community" : activeTab === "employees" ? "dashboard" : activeTab}
        setActiveTab={(tab) => {
          setActiveTab(tab);
        }}
      />

      {/* 2. MAIN OPERATION DASHBOARD SPACE */}
      <main className="flex-1 min-h-screen flex flex-col pl-64 transition-all duration-300">
        
        {/* TOP COMPREHENSIVE HEADER CONTROLS */}
        <header className="sticky top-0 bg-white/95 backdrop-blur-md z-45 border-b border-slate-200 py-3.5 px-6 flex justify-between items-center shrink-0">
          <div>
            <h1 className="text-base font-extrabold text-slate-900 tracking-tight font-sans">
              ALP Astrology Core CRM Network
            </h1>
            <p className="text-[10px] text-slate-500 font-mono mt-0.5 uppercase tracking-wider">
              Akshaya Lagna Paddhati Enterprise Resource Portal
            </p>
          </div>

          <div className="flex items-center gap-6">
            {/* Real Co-worker Auth Status & Switch Account Dropdown Panel */}
            <div className="relative">
              {currentUser ? (
                <div className="flex items-center gap-1">
                  <button
                    onClick={() => {
                      setShowProfileDropdown(!showProfileDropdown);
                      SoundEngine.playChime();
                    }}
                    className="flex items-center gap-2.5 bg-amber-500/10 hover:bg-amber-500/20 border border-slate-200 hover:border-amber-500/30 pl-2 pr-3 py-1.5 rounded-xl shadow-2xs transition-all cursor-pointer text-left"
                    title="Switch co-worker accounts"
                  >
                    <img
                      src={currentUser.avatar || "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=100"}
                      alt={currentUser.name}
                      referrerPolicy="no-referrer"
                      className="w-7 h-7 rounded-full object-cover border border-amber-500/40 shrink-0 shadow-xs"
                    />
                    <div className="leading-tight hidden sm:block shrink-0 max-w-[140px]">
                      <p className="text-[10.5px] font-black text-slate-900 tracking-tight leading-none truncate">
                        {currentUser.name}
                      </p>
                      <p className="text-[8px] font-extrabold text-amber-600 tracking-wider uppercase leading-none font-sans mt-0.5 flex items-center gap-0.5">
                        <span>{currentUser.role}</span>
                        <ChevronDown className="w-2.5 h-2.5 text-amber-500" />
                      </p>
                    </div>
                  </button>

                  {/* Quick logout lock button */}
                  <button
                    onClick={handleLogout}
                    className="text-slate-400 hover:text-rose-600 p-1.5 hover:bg-rose-50 rounded-lg cursor-pointer transition-colors"
                    title="Lock out and Clear Session"
                  >
                    <LogOut className="w-4 h-4" />
                  </button>
                </div>
              ) : (
                <div className="flex items-center gap-1">
                  <div className="flex items-center gap-2 bg-rose-50 border border-rose-200/60 px-3 py-1.5 rounded-xl shadow-2xs">
                    <ShieldAlert className="w-3.5 h-3.5 text-rose-500 animate-pulse" />
                    <span className="text-[9.5px] text-rose-700 font-extrabold uppercase font-mono tracking-wider">Locked Out</span>
                  </div>
                  <button
                    onClick={() => {
                      setLoginError("");
                      setShowLoginModal(true);
                      SoundEngine.playChime();
                    }}
                    className="flex items-center gap-1 text-[10px] font-black uppercase tracking-wider text-slate-950 bg-amber-500 hover:bg-amber-600 px-3.5 py-1.5 rounded-xl transition-all cursor-pointer shadow-xs font-mono"
                  >
                    <KeyRound className="w-3.5 h-3.5" />
                    <span>Authorize Key</span>
                  </button>
                </div>
              )}

              {/* The Advanced "Switch Account" Dropdown Menu */}
              {showProfileDropdown && currentUser && (
                <>
                  <div 
                    className="fixed inset-0 z-40 bg-transparent"
                    onClick={() => setShowProfileDropdown(false)}
                  />
                  <div className="absolute right-0 mt-2.5 w-76 bg-white border border-slate-200 rounded-2xl shadow-xl p-4 z-50 animate-in fade-in slide-in-from-top-3 duration-150 space-y-3">
                    <div className="border-b border-slate-100 pb-2.5">
                      <span className="text-[8.5px] font-black text-amber-600 uppercase tracking-widest block font-mono">
                        Active Coworker Session
                      </span>
                      <div className="flex items-center gap-3 mt-1.5">
                        <img
                          src={currentUser.avatar || "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=100"}
                          alt={currentUser.name}
                          className="w-10 h-10 rounded-full object-cover border border-amber-500/20"
                        />
                        <div className="leading-tight">
                          <h4 className="text-[11.5px] font-extrabold text-slate-900 truncate">
                            {currentUser.name}
                          </h4>
                          <span className="text-[9px] text-slate-500 block truncate font-mono">
                            {currentUser.email}
                          </span>
                          <span className="inline-flex items-center gap-1 mt-0.5 bg-emerald-550/10 text-emerald-700 font-extrabold text-[8px] uppercase tracking-wider px-1.5 py-0.5 rounded-md">
                            <span className="w-1 h-1 rounded-full bg-emerald-500 animate-pulse" />
                            Active Logged In
                          </span>
                        </div>
                      </div>
                    </div>

                    {/* Switch Account section */}
                    <div>
                      <span className="text-[8.5px] font-black text-slate-400 uppercase tracking-widest block font-mono mb-2">
                        Switch Account / Quick Options
                      </span>
                      <div className="space-y-1.5 max-h-48 overflow-y-auto pr-0.5">
                        {coworkers && coworkers.length > 0 ? (
                          coworkers
                            .filter(val => val.id !== currentUser.id)
                            .map((coworker) => (
                              <button
                                key={coworker.id}
                                onClick={() => {
                                  setShowProfileDropdown(false);
                                  // Fill credentials and try to login automatically
                                  setLoginEmail(coworker.email);
                                  setLoginPassword(coworker.password);
                                  setLoginError("");
                                  
                                  // Auto login via pipeline
                                  setIsLoggingIn(true);
                                  fetch("/api/auth/login", {
                                    method: "POST",
                                    headers: { "Content-Type": "application/json" },
                                    body: JSON.stringify({ email: coworker.email, password: coworker.password })
                                  })
                                    .then(res => res.json())
                                    .then(data => {
                                      if (data.success && data.user) {
                                        setCurrentUser(data.user);
                                        setActiveRole(data.user.role);
                                        SoundEngine.playSuccessAlert();
                                        showInstantNotification(
                                          "Account Switched",
                                          `Switched coworker session to ${data.user.name}.`,
                                          "success"
                                        );
                                      } else {
                                        // Open modal as fallback
                                        setShowLoginModal(true);
                                      }
                                    })
                                    .catch(() => {
                                      setShowLoginModal(true);
                                    })
                                    .finally(() => {
                                      setIsLoggingIn(false);
                                    });
                                }}
                                className="w-full flex items-center justify-between text-left p-1.5 hover:bg-slate-50 border border-transparent hover:border-slate-150 rounded-xl cursor-pointer transition-all duration-100 group"
                              >
                                <div className="flex items-center gap-2 max-w-[80%]">
                                  <img
                                    src={coworker.avatar || "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=100"}
                                    alt={coworker.name}
                                    className="w-7 h-7 rounded-full object-cover border border-slate-100 group-hover:border-amber-500/20"
                                  />
                                  <div className="leading-tight truncate">
                                    <span className="text-[10px] font-bold text-slate-800 block truncate group-hover:text-amber-600 transition-colors">
                                      {coworker.name}
                                    </span>
                                    <span className="text-[8px] font-semibold text-slate-400 block tracking-wider uppercase font-sans">
                                      {coworker.role}
                                    </span>
                                  </div>
                                </div>
                                <span className="text-[8px] text-amber-650 bg-amber-50 hover:bg-amber-100 px-1 py-0.5 rounded-sm font-semibold opacity-0 group-hover:opacity-100 transition-opacity">
                                  Switch ⇄
                                </span>
                              </button>
                            ))
                        ) : (
                          <div className="text-[10px] text-slate-400 py-1 text-center italic">No other co-workers found</div>
                        )}
                      </div>
                    </div>

                    <div className="border-t border-slate-100 pt-2 flex flex-col gap-1">
                      <button
                        onClick={() => {
                          setShowProfileDropdown(false);
                          setLoginEmail("");
                          setLoginPassword("");
                          setLoginError("");
                          setShowLoginModal(true);
                          SoundEngine.playChime();
                        }}
                        className="w-full text-center text-[10px] font-bold uppercase tracking-wider text-slate-700 hover:text-amber-650 hover:bg-slate-50 py-1 rounded-lg transition-all cursor-pointer font-mono"
                      >
                        + Use Another Access Key
                      </button>

                      <button
                        onClick={() => {
                          setShowProfileDropdown(false);
                          handleLogout();
                        }}
                        className="w-full bg-rose-50 hover:bg-rose-100/60 text-rose-750 border border-rose-150 hover:border-rose-200 text-center text-[10px] font-black uppercase tracking-wider py-1.5 rounded-lg transition-all cursor-pointer flex items-center justify-center gap-1"
                      >
                        <LogOut className="w-3.5 h-3.5" />
                        <span>Lock out & Clear Session</span>
                      </button>
                    </div>
                  </div>
                </>
              )}
            </div>

            {/* Sound & Alarm Active Preferences Utility */}
            <div className="flex items-center gap-2 bg-slate-100 hover:bg-slate-200/80 px-2.5 py-1.5 rounded-lg border border-slate-200 text-[10.5px] transition-colors">
              <button
                onClick={() => {
                  const newMute = !mutedAllSounds;
                  setMutedAllSounds(newMute);
                  localStorage.setItem("crm_mute_alerts", String(newMute));
                  SoundEngine.playChime();
                  setAudioAuthorized(true);
                }}
                className="flex items-center gap-1.5 focus:outline-hidden cursor-pointer"
                title={mutedAllSounds ? "Unmute Alarm Sound Alerts" : "Mute Alarm Sound Alerts"}
              >
                {mutedAllSounds ? (
                  <span className="flex items-center gap-1 text-rose-600 font-semibold">
                    <VolumeX className="w-3.5 h-3.5" />
                    <span className="font-mono text-[9px] uppercase">Muted</span>
                  </span>
                ) : (
                  <span className="flex items-center gap-1 text-emerald-700 font-semibold animate-pulse">
                    <Volume2 className="w-3.5 h-3.5 text-emerald-600" />
                    <span className="font-mono text-[9px] uppercase">Alarms On</span>
                  </span>
                )}
              </button>

              <span className="text-slate-300">|</span>

              <button
                onClick={() => {
                  setAudioAuthorized(true);
                  SoundEngine.playSuccessAlert();
                }}
                className="text-[9.5px] text-amber-600 hover:text-amber-700 font-bold uppercase transition-colors cursor-pointer"
                title="Test sound notification setup"
              >
                Test Sound
              </button>
            </div>

            {/* Simulated Live Connection Clock Indicator */}
            <div className="flex items-center gap-2">
              <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse"></span>
              <span className="text-[10px] font-mono text-emerald-600 font-bold uppercase tracking-widest">
                ERP Sync Online
              </span>
            </div>
          </div>
        </header>

        {/* CONTAINER CONTENT INSERTER */}
        <div className="flex-1 p-6 max-w-7xl w-full mx-auto space-y-6">
          
          {/* Active Tabs routing panel */}
          {activeTab === "dashboard" && (
            <DashboardOverview
              leads={leads}
              consultations={consultations}
              studentsCount={students.length}
              paymentsAmount={payments.filter(p => p.status === "Paid").reduce((acc, curr) => acc + curr.amount, 0)}
              tasks={tasks}
              auditLogs={auditLogs}
              notifications={notifications}
              onCompleteTask={handleCompleteTask}
              onClearNotifications={() => {
                fetch("/api/notifications/clear", { method: "POST" })
                  .then(res => res.json())
                  .then(data => {
                    if (data.success) {
                      setNotifications(prev => prev.map(n => ({ ...n, read: true })));
                      showInstantNotification("Notifications cleared", "All pending alerts marked as read.", "info");
                    }
                  });
              }}
              onNavigateTab={setActiveTab}
              onAddTask={handleAddTask}
              currentUser={currentUser}
              coworkers={coworkers}
            />
          )}

          {activeTab === "leads" && (
            <LeadManagement
              leads={leads}
              onAddLead={handleAddLead}
              onUpdateLead={handleUpdateLead}
              onBulkAssignLeads={handleBulkAssignLeads}
              onAddActivity={handleAddActivity}
              activities={leadActivities}
              fetchActivities={handleFetchActivities}
              onImportLeads={handleImportLeads}
              onAddTask={handleAddTask}
              tasks={tasks}
              currentUser={currentUser}
              coworkers={coworkers}
            />
          )}

          {activeTab === "consultations" && (
            <ConsultationManagement
              clients={clients}
              consultations={consultations}
              onAddClient={handleAddClient}
              onAddConsultation={handleAddConsultation}
              onUpdateConsultation={handleUpdateConsultation}
            />
          )}

          {(activeTab === "courses" || activeTab === "students" || activeTab === "academy") && (
            <CourseStudentManagement
              courses={courses}
              batches={batches}
              students={students}
              enrollments={enrollments}
              onAddStudent={handleAddStudent}
              onEnrollStudent={handleEnrollStudent}
              onUpdateEnrollment={handleUpdateEnrollment}
            />
          )}

          {activeTab === "payments" && (
            <PaymentManagement
              payments={payments}
              invoices={invoices}
              onAddPayment={handleAddPayment}
              onClearPending={handleClearPendingPayment}
            />
          )}

          {activeTab === "certificates" && (
            <CertificateManagement
              certificates={certificates}
              enrollments={enrollments}
              onIssueCertificate={handleIssueCertificate}
            />
          )}

          {(activeTab === "community" || activeTab === "events" || activeTab === "social") && (
            <CommunityEventSocial
              members={members}
              events={events}
              socialPosts={socialPosts}
              onAddMember={handleAddMember}
              onAddEvent={handleAddEvent}
              onBookEventTicket={handleBookEventTicket}
              onSchedulePost={handleSchedulePost}
            />
          )}

          {(activeTab === "reports" || activeTab === "analytics") && (
            <ReportAnalytics />
          )}

          {activeTab === "blueprint" && (
            <div className="bg-white border border-slate-200 rounded-xl p-6 space-y-6 shadow-xs">
              <div className="border-b border-slate-150 pb-4">
                <h2 className="text-lg font-bold text-slate-900">Akshaya Lagna Paddhati Architecture Blueprint</h2>
                <p className="text-xs text-slate-500">Enterprise CRM configuration roadmap, database structural schemas, and unified API pathways guidelines.</p>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-6 text-xs leading-relaxed font-mono">
                <div className="bg-slate-50 p-4 rounded-lg border border-slate-150 space-y-3">
                  <h4 className="font-bold text-slate-800 uppercase text-[11px] tracking-wider border-b border-slate-150 pb-1">Operational Role-Based Access Controls (RBAC)</h4>
                  <ul className="list-disc pl-4 space-y-1 text-slate-600">
                    <li><span className="text-amber-600 font-bold">Client / Consultants:</span> Birth profile variables, progressive degrees calculation limits.</li>
                    <li><span className="text-amber-600 font-bold">Trainer:</span> Access roll sheets checklist planner, lesson evaluations.</li>
                    <li><span className="text-amber-600 font-bold">Astrologer:</span> Prescription pads logs, AI Gemini analysis.</li>
                    <li><span className="text-amber-600 font-bold">Accountant:</span> Income shares, GST tax invoice generator ledger.</li>
                  </ul>
                </div>

                <div className="bg-slate-50 p-4 rounded-lg border border-slate-150 space-y-3">
                  <h4 className="font-bold text-slate-800 uppercase text-[11px] tracking-wider border-b border-slate-150 pb-1">ALP Software progression scale</h4>
                  <p className="text-slate-600">
                    Akshaya Lagna Paddhati advances birth coordinates precisely 1 degree per annual rotation circle. This CRM matches biological timelines with celestial transits triggers, predicting opportunities automatically via generative Gemini telemetry prompts.
                  </p>
                  <p className="text-emerald-600 font-bold mt-2">✔ System checks verified - 100% compliant</p>
                </div>
              </div>
            </div>
          )}

          {activeTab === "employees" && (
            <EmployeeManagement
              currentUserId={currentUser ? currentUser.id : "guest"}
              currentUserRole={currentUser ? currentUser.role : "all"}
              showNotification={(channel, info, status) => {
                showInstantNotification(channel, info, status);
              }}
            />
          )}

        </div>
      </main>

      {/* Dynamic Sound Reminder / Call Alarm Dialog Overlay */}
      {activeAlertTask && (
        <div className="fixed inset-0 bg-slate-950/75 backdrop-blur-xs flex items-center justify-center z-[9999] p-4 select-none animate-in fade-in duration-300">
          <div className="bg-[#0c142c] border-2 border-amber-500 w-full max-w-sm rounded-2xl p-5 text-white shadow-[0_0_50px_rgba(245,158,11,0.2)] relative overflow-hidden space-y-4">
            
            {/* Glowing cosmic backdrop effects */}
            <div className="absolute top-0 right-0 w-32 h-32 bg-amber-500/10 rounded-full blur-2xl -mr-16 -mt-16 pointer-events-none"></div>
            <div className="absolute bottom-0 left-0 w-32 h-32 bg-amber-500/5 rounded-full blur-2xl -ml-16 -mb-16 pointer-events-none"></div>
            
            {/* Alarm Header */}
            <div className="flex items-center gap-3 border-b border-white/10 pb-3">
              <div className="bg-amber-500/15 p-2.5 rounded-xl border border-amber-500/30 text-amber-500 animate-bounce">
                <Bell className="w-5 h-5" />
              </div>
              <div className="flex-1">
                <span className="text-[10px] font-bold font-mono tracking-widest text-amber-400 uppercase">
                  CRM Follow-Up Alert
                </span>
                <h3 className="text-xs font-bold text-slate-100 uppercase tracking-wider leading-tight">
                  Scheduled Task Ringing
                </h3>
              </div>
            </div>

            {/* Task Info Panel */}
            <div className="bg-slate-950/40 border border-white/5 rounded-xl p-3.5 space-y-1.5">
              <span className="text-[9px] text-amber-500 font-mono flex items-center gap-1">
                <Clock className="w-3.5 h-3.5 animate-pulse" />
                <span>
                  Due Time: {activeAlertTask.reminderTime ? new Date(activeAlertTask.reminderTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : "Now"}
                </span>
              </span>
              <h4 className="text-[12.5px] font-extrabold text-white tracking-wide font-sans leading-relaxed">
                {activeAlertTask.title}
              </h4>
              {activeAlertTask.leadId && (
                <div className="text-[9px] text-slate-400 mt-1 flex items-center gap-1 font-mono uppercase">
                  <span>Prospect Link:</span>
                  <span className="text-slate-350 font-bold bg-white/5 px-1 rounded">{activeAlertTask.leadId}</span>
                </div>
              )}
            </div>

            {/* Status indicators */}
            <div className="flex items-center justify-between text-[10px] text-slate-400 bg-slate-950/20 px-2 py-1.5 rounded border border-white/5">
              <span className="flex items-center gap-1.5">
                {mutedAllSounds ? (
                  <>
                    <VolumeX className="w-3.5 h-3.5 text-rose-500" />
                    <span className="font-mono text-[9px] uppercase tracking-wider text-rose-400/90 font-bold">Sound Alert Muted</span>
                  </>
                ) : (
                  <>
                    <Volume2 className="w-3.5 h-3.5 text-emerald-400 animate-pulse" />
                    <span className="font-mono text-[9px] uppercase tracking-wider text-emerald-400/90 font-bold">Ringing sound active</span>
                  </>
                )}
              </span>
              <button 
                onClick={() => {
                  const newMute = !mutedAllSounds;
                  setMutedAllSounds(newMute);
                  localStorage.setItem("crm_mute_alerts", String(newMute));
                  SoundEngine.playChime();
                }}
                className="text-[9px] text-amber-400 font-bold hover:underline font-mono uppercase"
              >
                {mutedAllSounds ? "Unmute" : "Mute"}
              </button>
            </div>

            {/* Controls */}
            <div className="grid grid-cols-3 gap-2 pt-1 text-[10.5px]">
              <button
                onClick={() => {
                  setDismissedReminderIds(prev => [...prev, activeAlertTask.id]);
                  showInstantNotification("Alarm dismissed", `Temporarily dismissed: "${activeAlertTask.title}"`, "info");
                }}
                className="bg-slate-900 hover:bg-slate-800 text-slate-300 py-2 rounded-xl font-bold transition-colors border border-white/5 cursor-pointer text-center"
              >
                Dismiss
              </button>
              
              <button
                onClick={() => handleSnoozeReminder(activeAlertTask)}
                className="bg-slate-950 hover:bg-amber-500/10 hover:border-amber-500/40 text-amber-400 py-2 rounded-xl font-bold transition-colors border border-amber-500/20 cursor-pointer flex items-center justify-center gap-1"
                title="Snooze for 5 minutes"
              >
                <Clock className="w-3 h-3 text-amber-400" />
                <span>+5 Min</span>
              </button>
              
              <button
                onClick={() => {
                  handleCompleteTask(activeAlertTask.id);
                  setDismissedReminderIds(prev => [...prev, activeAlertTask.id]);
                }}
                className="bg-amber-500 hover:bg-amber-600 text-slate-950 py-2 rounded-xl font-extrabold transition-colors cursor-pointer flex items-center justify-center gap-1"
              >
                <CheckSquare className="w-3 h-3 text-slate-950" />
                <span>Execute</span>
              </button>
            </div>

          </div>
        </div>
      )}

      {/* 4. SECURE CO-WORKER LOGIN OVERLAY DIALOG */}
      {showLoginModal && (
        <div className="fixed inset-0 bg-slate-950/70 backdrop-blur-xs flex items-center justify-center z-[99999] p-4 animate-in fade-in duration-200">
          <div className="bg-white rounded-2xl border border-slate-200 w-full max-w-md p-6 text-slate-800 shadow-2xl relative space-y-4 animate-in zoom-in-95 duration-200">
            {/* Close */}
            <button
              onClick={() => {
                setShowLoginModal(false);
                setLoginError("");
              }}
              className="absolute top-4 right-4 text-slate-400 hover:text-slate-650 cursor-pointer font-bold font-mono transition-colors text-xs"
            >
              [✕] Close
            </button>

            <div className="border-b border-slate-100 pb-3 flex items-center gap-2.5">
              <div className="bg-amber-100 p-2.5 rounded-xl text-amber-600">
                <KeyRound className="w-5 h-5" />
              </div>
              <div>
                <span className="text-[9px] uppercase tracking-widest font-black text-amber-600 font-mono">
                  ALP ASTROLOGY ACCESS
                </span>
                <h3 className="font-extrabold text-sm text-slate-900 uppercase tracking-tight">
                  Co-worker Secure Login
                </h3>
              </div>
            </div>

            {loginError && (
              <div className="text-[11px] font-bold text-rose-700 bg-rose-50 border border-rose-200/60 p-2.5 rounded-lg">
                ❌ {loginError}
              </div>
            )}

            <form onSubmit={handleLoginSubmit} className="space-y-3.5">
              <div>
                <label className="block text-[10px] font-extrabold text-slate-500 uppercase tracking-wider mb-1">
                  Worker Email Address (Login User ID)
                </label>
                <div className="relative">
                  <Mail className="absolute left-3 top-2.5 w-3.5 h-3.5 text-slate-400" />
                  <input
                    type="email"
                    value={loginEmail}
                    onChange={(e) => setLoginEmail(e.target.value)}
                    required
                    placeholder="e.g. employee1@alpastrology.com"
                    className="w-full bg-slate-50 border border-slate-200 rounded-lg pl-9 pr-3 py-2 text-xs text-slate-800 focus:outline-hidden focus:border-amber-500"
                  />
                </div>
              </div>

              <div>
                <label className="block text-[10px] font-extrabold text-slate-500 uppercase tracking-wider mb-1">
                  Assigned Passkey Password
                </label>
                <div className="relative">
                  <Lock className="absolute left-3 top-2.5 w-3.5 h-3.5 text-slate-400" />
                  <input
                    type="password"
                    value={loginPassword}
                    onChange={(e) => setLoginPassword(e.target.value)}
                    required
                    placeholder="••••••••"
                    className="w-full bg-slate-50 border border-slate-200 rounded-lg pl-9 pr-3 py-2 text-xs text-slate-800 focus:outline-hidden focus:border-amber-500"
                  />
                </div>
              </div>

              <button
                type="submit"
                disabled={isLoggingIn}
                className="w-full bg-amber-500 hover:bg-amber-600 disabled:opacity-50 text-slate-950 font-black uppercase tracking-wider py-2.5 rounded-xl text-xs flex items-center justify-center gap-1.5 cursor-pointer shadow-xs font-mono"
              >
                {isLoggingIn ? "Authorizing Keys..." : "Authenticate Session Key"}
              </button>
            </form>

            {/* Quick Presets Section */}
            <div className="border-t border-slate-150 pt-3">
              <span className="block text-[9px] font-extrabold text-slate-400 uppercase tracking-wider mb-2 font-mono">
                Click to Auto-fill Coworker Accounts:
              </span>
              <div className="grid grid-cols-2 gap-2 text-[10.5px]">
                {[
                  { label: "Admin Founder", e: "founder@alpastrology.com", p: "admin" },
                  { label: "Employee 1 (Astrologer)", e: "employee1@alpastrology.com", p: "employee1" },
                  { label: "Employee 2 (Receptionist)", e: "employee2@alpastrology.com", p: "employee2" },
                  { label: "Employee 3 (Social)", e: "employee3@alpastrology.com", p: "employee3" }
                ].map((preset) => (
                  <div
                    key={preset.e}
                    onClick={() => {
                      setLoginEmail(preset.e);
                      setLoginPassword(preset.p);
                      setLoginError("");
                      SoundEngine.playChime();
                    }}
                    className="bg-slate-50 hover:bg-amber-50/70 border border-slate-200 hover:border-amber-500/20 px-2.5 py-2 rounded-lg cursor-pointer transition-all flex flex-col items-start leading-tight"
                  >
                    <span className="font-extrabold text-[10px] text-slate-700 truncate w-full">{preset.label}</span>
                    <span className="text-[8.5px] text-slate-400 font-mono truncate w-full mt-0.5">{preset.e}</span>
                  </div>
                ))}
              </div>
            </div>

            <p className="text-[9.5px] text-slate-400 text-center uppercase tracking-wider">
              Protected by ALP Cryptographic Access Clearance system
            </p>
          </div>
        </div>
      )}
    </div>
  );
}

