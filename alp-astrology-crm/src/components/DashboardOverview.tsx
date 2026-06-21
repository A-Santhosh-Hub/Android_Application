import React from "react";
import {
  Users,
  Sparkles,
  GraduationCap,
  TrendingUp,
  Clock,
  ArrowRight,
  CheckCircle,
  AlertCircle,
  Plus,
  Phone,
  Bell,
  Play,
  Check,
  Zap,
  Volume2,
  Trash
} from "lucide-react";
import { Lead, Consultation, Task, AuditLog, Notification } from "../types";
import { SoundEngine } from "../utils/sound";

interface DashboardOverviewProps {
  leads: Lead[];
  consultations: Consultation[];
  studentsCount: number;
  paymentsAmount: number;
  tasks: Task[];
  auditLogs: AuditLog[];
  notifications: Notification[];
  onCompleteTask: (taskId: string) => void;
  onClearNotifications: () => void;
  onNavigateTab: (tabId: string) => void;
  onAddTask: (task: Partial<Task>) => Promise<any>;
  currentUser?: any;
  coworkers?: any[];
}

export function DashboardOverview({
  leads,
  consultations,
  studentsCount,
  paymentsAmount,
  tasks,
  auditLogs,
  notifications,
  onCompleteTask,
  onClearNotifications,
  onNavigateTab,
  onAddTask,
  currentUser,
  coworkers = []
}: DashboardOverviewProps) {
  const [taskFilter, setTaskFilter] = React.useState<"all" | "personal">(currentUser ? "personal" : "all");

  const pendingTasks = tasks.filter(t => {
    if (t.status !== "Pending") return false;
    if (taskFilter === "personal" && currentUser) {
      const taskAssignee = (t.assignedTo || "").toLowerCase();
      const userName = (currentUser.name || "").toLowerCase();
      const userRole = (currentUser.role || "").toLowerCase();
      // Match by coworker name or role
      return taskAssignee.includes(userName) || userName.includes(taskAssignee) || taskAssignee.includes(userRole);
    }
    return true;
  });

  const unreadNotifications = notifications.filter(n => !n.read);

  // Custom Task / Sound Alarm Scheduler States
  const [showScheduler, setShowScheduler] = React.useState(false);
  const [taskTitle, setTaskTitle] = React.useState("");
  const [taskPriority, setTaskPriority] = React.useState<"Low" | "Medium" | "High">("Medium");
  const [assignedTo, setAssignedTo] = React.useState(currentUser ? currentUser.name : "Receptionist");
  const [alarmEnabled, setAlarmEnabled] = React.useState(true);
  const [alarmTime, setAlarmTime] = React.useState("");
  const [soundPreset, setSoundPreset] = React.useState("task"); // 'call', 'task', 'chime', 'siren'
  const [isSubmitting, setIsSubmitting] = React.useState(false);

  React.useEffect(() => {
    if (currentUser) {
      setAssignedTo(currentUser.name);
      setTaskFilter("personal");
    } else {
      setAssignedTo("Receptionist");
      setTaskFilter("all");
    }
  }, [currentUser]);

  const setQuickAlarmSeconds = (secs: number) => {
    const targetDate = new Date(Date.now() + secs * 1000);
    const offset = targetDate.getTimezoneOffset() * 60000;
    const localISOTime = new Date(targetDate.getTime() - offset).toISOString().slice(0, 16);
    setAlarmTime(localISOTime);
    setAlarmEnabled(true);
  };

  const setQuickAlarmMinutes = (mins: number) => {
    const targetDate = new Date(Date.now() + mins * 60 * 1000);
    const offset = targetDate.getTimezoneOffset() * 60000;
    const localISOTime = new Date(targetDate.getTime() - offset).toISOString().slice(0, 16);
    setAlarmTime(localISOTime);
    setAlarmEnabled(true);
  };

  const handleFormSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!taskTitle.trim()) return;

    setIsSubmitting(true);

    // Dynamic identifier tagging based on selected sound alarm preset
    let finalTitle = taskTitle.trim();
    if (soundPreset === "call") {
      finalTitle = `📞 [Call]: ${finalTitle}`;
    } else if (soundPreset === "chime") {
      finalTitle = `📅 [Meeting Chime]: ${finalTitle}`;
    } else if (soundPreset === "siren") {
      finalTitle = `⚠️ [Urgent Siren]: ${finalTitle}`;
    } else {
      finalTitle = `📌 [Task Arpeggio]: ${finalTitle}`;
    }

    const payload: Partial<Task> = {
      title: finalTitle,
      priority: taskPriority,
      assignedTo: assignedTo,
      dueDate: new Date().toISOString().split("T")[0],
      isReminder: alarmEnabled,
      reminderTime: alarmEnabled && alarmTime ? new Date(alarmTime).toISOString() : undefined,
      status: "Pending"
    };

    onAddTask(payload)
      .then(() => {
        setTaskTitle("");
        setAlarmTime("");
        setShowScheduler(false);
        // Play success chord tone
        SoundEngine.playSuccessAlert();
      })
      .catch(err => console.error("Sound scheduler failure", err))
      .finally(() => {
        setIsSubmitting(false);
      });
  };

  // Compute stat highlights
  const rawRevenue = paymentsAmount;
  const formattedRevenue = new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    maximumFractionDigits: 0
  }).format(rawRevenue);

  const stats = [
    {
      title: "Total Prospects",
      value: leads.length,
      change: "+24% this week",
      icon: Users,
      color: "bg-amber-100/50 text-amber-700",
      borderColor: "border-slate-200"
    },
    {
      title: "Consultations Scheduled",
      value: consultations.filter(c => c.status === "Scheduled").length,
      change: `${consultations.filter(c => c.status === "Completed").length} completed`,
      icon: Sparkles,
      color: "bg-purple-100/50 text-purple-700",
      borderColor: "border-slate-200"
    },
    {
      title: "Active Students",
      value: studentsCount,
      change: "Across 2 cohorts",
      icon: GraduationCap,
      color: "bg-emerald-100/50 text-emerald-700",
      borderColor: "border-slate-200"
    }
  ];

  return (
    <div className="space-y-6">
      {/* Upper Welcome Header */}
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4 border-b border-slate-200 pb-4">
        <div>
          <h2 className="text-lg font-bold text-slate-900 tracking-tight">Akshaya Lagna Paddhati Dashboard</h2>
          <p className="text-xs text-slate-500">Real-time enterprise intelligence and role operations tracking console.</p>
        </div>
        {unreadNotifications.length > 0 && (
          <button
            onClick={onClearNotifications}
            className="flex items-center gap-2 bg-amber-500/10 text-amber-600 hover:bg-amber-500 hover:text-slate-950 border border-amber-500/20 px-3.5 py-1.5 rounded-lg text-xs font-semibold transition-all cursor-pointer"
          >
            <CheckCircle className="w-3.5 h-3.5" />
            <span>Mark {unreadNotifications.length} alerts read</span>
          </button>
        )}
      </div>

      {/* Bento Stats Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        {stats.map((item, idx) => {
          const Icon = item.icon;
          return (
            <div
              key={idx}
              className={`bg-white border ${item.borderColor} rounded-xl p-4 shadow-xs flex flex-col justify-between transition-transform duration-200 hover:-translate-y-0.5`}
            >
              <div className="flex items-center justify-between">
                <span className="text-[10px] font-bold text-slate-500 tracking-wider uppercase">{item.title}</span>
                <div className={`p-1.5 rounded-lg border border-slate-100 ${item.color}`}>
                  <Icon className="w-3.5 h-3.5" />
                </div>
              </div>
              <div className="mt-3">
                <h3 className="text-2xl font-bold font-sans text-slate-900 leading-tight">{item.value}</h3>
                <p className="text-[10px] text-slate-500 mt-0.5 flex items-center gap-1">
                  <TrendingUp className="w-3 h-3 text-emerald-600 shrink-0" />
                  <span>{item.change}</span>
                </p>
              </div>
            </div>
          );
        })}
      </div>

      {/* Main Split Console Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left 2 Column Focus Panels */}
        <div className="lg:col-span-2 space-y-6">
          {/* Today's Follow-ups and Scheduler Alerts */}
          <div className="bg-white border border-slate-200 rounded-xl p-5 shadow-xs space-y-4">
            <div className="flex flex-col sm:flex-row justify-between sm:items-center gap-2 border-b border-slate-100 pb-3">
              <div className="flex items-center gap-2">
                <Clock className="w-4 h-4 text-rose-500" />
                <h3 className="font-bold text-sm text-slate-800">Pending Actions & Follow-ups</h3>
              </div>
              <div className="flex items-center gap-3">
                {currentUser && (
                  <div className="bg-slate-100/90 p-0.5 rounded-lg border border-slate-200 inline-flex items-center text-[10px]">
                    <button
                      type="button"
                      onClick={() => {
                        setTaskFilter("personal");
                        SoundEngine.playChime();
                      }}
                      className={`px-2.5 py-1 font-extrabold rounded-md cursor-pointer transition-colors ${
                        taskFilter === "personal"
                          ? "bg-amber-500 text-slate-950 shadow-2xs"
                          : "text-slate-600 hover:text-slate-900"
                      }`}
                    >
                      👤 Mine
                    </button>
                    <button
                      type="button"
                      onClick={() => {
                        setTaskFilter("all");
                        SoundEngine.playChime();
                      }}
                      className={`px-2.5 py-1 font-extrabold rounded-md cursor-pointer transition-colors ${
                        taskFilter === "all"
                          ? "bg-slate-900 text-white shadow-2xs"
                          : "text-slate-600 hover:text-white"
                      }`}
                    >
                      🌐 All
                    </button>
                  </div>
                )}

                <button
                  type="button"
                  onClick={() => {
                    setShowScheduler(!showScheduler);
                    SoundEngine.playChime();
                  }}
                  className="flex items-center gap-1.5 text-[10.5px] font-bold text-amber-700 bg-amber-50 hover:bg-amber-100/80 px-2.5 py-1 rounded-lg border border-amber-500/20 transition-all cursor-pointer"
                >
                  <Plus className="w-3.5 h-3.5" />
                  <span>{showScheduler ? "Hide Scheduler" : "Schedule Custom Alarm"}</span>
                </button>
                <span className="text-[9.5px] font-bold text-rose-600 bg-rose-50 border border-rose-100 px-2 py-0.5 rounded-full uppercase shrink-0">
                  {pendingTasks.length} Pending
                </span>
              </div>
            </div>

            {/* Custom Task Sound Alarm & Reminder Scheduler Drawer */}
            {showScheduler && (
              <form onSubmit={handleFormSubmit} className="bg-slate-50 border border-slate-200 rounded-xl p-4 space-y-4 shadow-inner animate-in fade-in duration-300">
                <div className="flex items-center justify-between border-b border-slate-200/80 pb-2">
                  <h4 className="text-[11px] font-extrabold text-slate-700 uppercase tracking-wider flex items-center gap-1.5">
                    <Bell className="w-3.5 h-3.5 text-amber-600 animate-pulse" />
                    <span>Worker Task & Alarm Precision Scheduler</span>
                  </h4>
                  <span className="text-[9.5px] text-slate-400 font-mono">Set alerts on the fly</span>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  {/* Left Column: Title and options */}
                  <div className="space-y-3">
                    <div>
                      <label className="block text-[10px] font-bold text-slate-600 uppercase mb-1">Task / Follow-up Title</label>
                      <input
                        type="text"
                        value={taskTitle}
                        onChange={(e) => setTaskTitle(e.target.value)}
                        placeholder="e.g. Call client regarding horoscope prediction match"
                        required
                        className="w-full bg-white border border-slate-200 rounded-lg px-3 py-2 text-xs text-slate-800 placeholder-slate-400 focus:outline-hidden focus:border-amber-500 focus:ring-1 focus:ring-amber-500/20"
                      />
                    </div>

                    <div className="grid grid-cols-2 gap-3">
                      <div>
                        <label className="block text-[10px] font-bold text-slate-600 uppercase mb-1">Priority</label>
                        <select
                          value={taskPriority}
                          onChange={(e: any) => setTaskPriority(e.target.value)}
                          className="w-full bg-white border border-slate-200 rounded-lg px-2.5 py-1.5 text-xs text-slate-700 cursor-pointer focus:outline-hidden focus:border-amber-500"
                        >
                          <option value="Low">Low</option>
                          <option value="Medium">Medium</option>
                          <option value="High">High</option>
                        </select>
                      </div>

                      <div>
                        <label className="block text-[10px] font-bold text-slate-600 uppercase mb-1">Assigned Worker</label>
                        <select
                          value={assignedTo}
                          onChange={(e) => setAssignedTo(e.target.value)}
                          className="w-full bg-white border border-slate-200 rounded-lg px-2.5 py-1.5 text-xs text-slate-700 cursor-pointer focus:outline-none focus:border-amber-500"
                        >
                          {coworkers && coworkers.length > 0 ? (
                            coworkers.map(c => (
                              <option key={c.id} value={c.name}>{c.name} ({c.role})</option>
                            ))
                          ) : (
                            <>
                              <option value="Receptionist">Receptionist</option>
                              <option value="Astrologer Advisor">Astrologer Advisor</option>
                              <option value="Operations Admin">Operations Admin</option>
                              <option value="Social Lead Agent">Social Lead Agent</option>
                            </>
                          )}
                        </select>
                      </div>
                    </div>
                  </div>

                  {/* Right Column: Time offsets and Sound selector */}
                  <div className="space-y-3">
                    <div>
                      <div className="flex items-center justify-between mb-1">
                        <label className="block text-[10px] font-bold text-slate-600 uppercase">Alarm Trigger Time</label>
                        <div className="flex items-center gap-1.5">
                          <input
                            type="checkbox"
                            id="alarmEnabled"
                            checked={alarmEnabled}
                            onChange={(e) => setAlarmEnabled(e.target.checked)}
                            className="w-3 h-3 text-amber-500 border-slate-300 rounded focus:ring-amber-500"
                          />
                          <label htmlFor="alarmEnabled" className="text-[9.5px] font-bold text-amber-700 cursor-pointer">Enable Audio Alert</label>
                        </div>
                      </div>
                      
                      <input
                        type="datetime-local"
                        value={alarmTime}
                        onChange={(e) => setAlarmTime(e.target.value)}
                        disabled={!alarmEnabled}
                        required={alarmEnabled}
                        className="w-full bg-white border border-slate-200 disabled:opacity-40 rounded-lg px-3 py-1.5 text-xs text-slate-800 focus:outline-hidden focus:border-amber-500"
                      />

                      {alarmEnabled && (
                        <div className="mt-1.5 flex flex-wrap gap-1 items-center">
                          <span className="text-[9px] text-slate-400 font-bold mr-1">Quick Presets:</span>
                          <button
                            type="button"
                            onClick={() => setQuickAlarmSeconds(30)}
                            className="bg-slate-200/80 hover:bg-slate-300 text-slate-700 text-[8.5px] font-mono font-bold px-1.5 py-0.5 rounded cursor-pointer transition-colors"
                          >
                            +30 sec
                          </button>
                          <button
                            type="button"
                            onClick={() => setQuickAlarmMinutes(1)}
                            className="bg-slate-200/80 hover:bg-slate-300 text-slate-700 text-[8.5px] font-mono font-bold px-1.5 py-0.5 rounded cursor-pointer transition-colors"
                          >
                            +1 min
                          </button>
                          <button
                            type="button"
                            onClick={() => setQuickAlarmMinutes(5)}
                            className="bg-slate-200/80 hover:bg-slate-300 text-slate-700 text-[8.5px] font-mono font-bold px-1.5 py-0.5 rounded cursor-pointer transition-colors"
                          >
                            +5 min
                          </button>
                          <button
                            type="button"
                            onClick={() => setQuickAlarmMinutes(60)}
                            className="bg-slate-200/80 hover:bg-slate-300 text-slate-700 text-[8.5px] font-mono font-bold px-1.5 py-0.5 rounded cursor-pointer transition-colors"
                          >
                            +1 hr
                          </button>
                        </div>
                      )}
                    </div>

                    <div>
                      <label className="block text-[10px] font-bold text-slate-600 uppercase mb-1.5">Selected Alarm Ringtone</label>
                      <div className="grid grid-cols-2 gap-2 text-[10px]">
                        {[
                          { id: "call", name: "📞 Phone Ring Alert (Call)", play: () => SoundEngine.playCallAlert() },
                          { id: "task", name: "🔔 Ascending Arpeggio (Task)", play: () => SoundEngine.playTaskReminder() },
                          { id: "chime", name: "⏰ Clock Chime (Meeting)", play: () => SoundEngine.playChime() },
                          { id: "siren", name: "⚠️ Attention Sawtooth (Urgent)", play: () => SoundEngine.playWarningSiren() }
                        ].map((sound) => (
                          <div
                            key={sound.id}
                            onClick={() => {
                              if (alarmEnabled) {
                                setSoundPreset(sound.id);
                                sound.play();
                              }
                            }}
                            className={`flex items-center justify-between p-1.5 rounded-lg border cursor-pointer select-none transition-all ${
                              !alarmEnabled
                                ? "bg-slate-100 text-slate-400 border-slate-150 cursor-not-allowed"
                                : soundPreset === sound.id
                                ? "bg-amber-500/10 border-amber-500 text-amber-800 font-semibold"
                                : "bg-white border-slate-250 text-slate-600 hover:bg-slate-100"
                            }`}
                          >
                            <span className="truncate">{sound.name}</span>
                            <button
                              type="button"
                              disabled={!alarmEnabled}
                              onClick={(e) => {
                                e.stopPropagation();
                                sound.play();
                              }}
                              className="p-1 rounded bg-slate-200/60 hover:bg-amber-500 hover:text-slate-950 transition-colors cursor-pointer"
                              title="Test sound preview"
                            >
                              <Play className="w-2.5 h-2.5" />
                            </button>
                          </div>
                        ))}
                      </div>
                    </div>
                  </div>
                </div>

                <div className="flex justify-end gap-2 border-t border-slate-200/60 pt-3">
                  <button
                    type="button"
                    onClick={() => {
                      setShowScheduler(false);
                      SoundEngine.playChime();
                    }}
                    className="border border-slate-250 hover:bg-slate-100 text-slate-600 px-3.5 py-1.5 rounded-lg text-xs font-semibold cursor-pointer"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    disabled={isSubmitting}
                    className="bg-amber-500 hover:bg-amber-600 disabled:opacity-50 text-slate-950 px-4 py-1.5 rounded-lg text-xs font-extrabold flex items-center gap-1 cursor-pointer"
                  >
                    {isSubmitting ? (
                      <span className="w-3 h-3 border-2 border-slate-950 border-t-transparent rounded-full animate-spin"></span>
                    ) : (
                      <Check className="w-3.5 h-3.5" />
                    )}
                    <span>Arm Custom Task Alarm</span>
                  </button>
                </div>
              </form>
            )}

            {pendingTasks.length === 0 ? (
              <div className="text-center py-8 text-xs text-slate-500">
                <CheckCircle className="w-8 h-8 text-emerald-500/30 mx-auto mb-2" />
                No pending CRM calls or tasks active. All clear!
              </div>
            ) : (
              <div className="space-y-2 max-h-72 overflow-y-auto pr-1">
                {pendingTasks.map(task => (
                  <div
                    key={task.id}
                    className="flex justify-between items-center p-3 rounded-lg bg-slate-50 border border-slate-150 hover:border-slate-300 transition-all text-xs"
                  >
                    <div className="space-y-0.5 pr-4">
                      <p className="text-slate-800 font-semibold">{task.title}</p>
                      <div className="flex items-center gap-2 text-[10px] text-slate-500 font-mono">
                        <span className="text-amber-600 font-bold">{task.priority} Priority</span>
                        <span>•</span>
                        <span>Due: {task.dueDate}</span>
                        <span>•</span>
                        <span>Agent: {task.assignedTo}</span>
                      </div>
                    </div>
                    <button
                      onClick={() => onCompleteTask(task.id)}
                      className="shrink-0 bg-emerald-500/10 hover:bg-emerald-600 hover:text-white text-emerald-700 border border-emerald-500/20 px-3 py-1.5 rounded-md text-[10px] font-bold transition-all cursor-pointer"
                    >
                      Complete
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Quick Active Consultations Pipeline */}
          <div className="bg-white border border-slate-200 rounded-xl p-5 shadow-xs">
            <div className="flex justify-between items-center mb-3.5">
              <h3 className="font-bold text-sm text-slate-800">Upcoming Active Consultations</h3>
              <button
                onClick={() => onNavigateTab("consultations")}
                className="text-xs text-amber-600 hover:text-amber-700 font-bold flex items-center gap-1 transition-colors cursor-pointer"
              >
                <span>Schedules calendar</span>
                <ArrowRight className="w-3.5 h-3.5" />
              </button>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
              {consultations
                .filter(c => c.status === "Scheduled")
                .slice(0, 4)
                .map(meet => (
                  <div
                    key={meet.id}
                    className="bg-slate-50 border border-slate-150 rounded-lg p-3 space-y-2 text-xs hover:border-amber-500/30 transition-colors"
                  >
                    <div className="flex justify-between items-start gap-2">
                      <span className="font-bold text-slate-800 truncate">{meet.clientName}</span>
                      <span className="text-[9px] font-bold bg-amber-500/10 text-amber-700 border border-amber-500/20 px-1.5 py-0.5 rounded-xs">
                        {meet.type}
                      </span>
                    </div>
                    <div className="space-y-0.5 text-slate-500 font-mono text-[10px]">
                      <p>Time: {new Date(meet.dateTime).toLocaleString([], { dateStyle: "short", timeStyle: "short" })}</p>
                      <p className="truncate">Advisor: {meet.astrologerName}</p>
                    </div>
                    <a
                      href={meet.meetingLink}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="block text-center bg-slate-800 hover:bg-slate-700 text-slate-100 rounded-md py-1.5 transition-colors tracking-wide text-[10px] font-bold"
                    >
                      Launch Tele-Session
                    </a>
                  </div>
                ))}
            </div>
          </div>
        </div>

        {/* Right 1 Column System Log Alerts */}
        <div className="space-y-6">
          {/* Real-time System Notifications Alerts */}
          <div className="bg-white border border-slate-200 rounded-xl p-5 shadow-xs">
            <h3 className="font-bold text-sm text-slate-800 mb-3.5 flex items-center gap-2">
              <AlertCircle className="w-4 h-4 text-amber-500" />
              <span>Real-time System Alerts</span>
            </h3>

            <div className="space-y-2 select-none max-h-56 overflow-y-auto pr-1">
              {notifications.map(n => (
                <div
                  key={n.id}
                  className={`p-3 rounded-lg border text-xs transition-colors ${
                    !n.read
                      ? "bg-amber-500/5 border-amber-500/20 text-slate-800"
                      : "bg-slate-50 border-slate-150 text-slate-500"
                  }`}
                >
                  <div className="flex justify-between items-start gap-2">
                    <p className={`font-bold ${!n.read ? "text-amber-700" : "text-slate-700"}`}>{n.title}</p>
                    <span className="text-[9px] text-slate-400 font-mono shrink-0">
                      {new Date(n.timestamp).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}
                    </span>
                  </div>
                  <p className="text-[11px] mt-0.5 text-slate-600 leading-snug">{n.message}</p>
                </div>
              ))}
            </div>
          </div>

          {/* Audit Logs Trail */}
          <div className="bg-white border border-slate-200 rounded-xl p-5 shadow-xs flex flex-col h-[280px]">
            <h3 className="font-bold text-sm text-slate-800 mb-2.5 font-sans">Security Audit Trail</h3>
            <div className="flex-1 overflow-y-auto space-y-2 pr-1 font-mono text-[10px] text-slate-500">
              {auditLogs.map((log, lIdx) => (
                <div key={log.id || lIdx} className="border-b border-slate-100 pb-2 last:border-0">
                  <div className="flex justify-between items-center text-slate-400 text-[9px] mb-0.5">
                    <span>{log.userName} ({log.userRole})</span>
                    <span>
                      {new Date(log.timestamp).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}
                    </span>
                  </div>
                  <p className="text-slate-700 text-[10px]">
                    <span className="text-amber-700 font-semibold">{log.action}: </span>
                    {log.target}
                  </p>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
