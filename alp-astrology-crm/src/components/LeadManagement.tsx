import React from "react";
import {
  Plus,
  Filter,
  User,
  MessageSquare,
  Phone,
  Video,
  FileText,
  Building,
  MoreVertical,
  ArrowRightLeft,
  Briefcase,
  X,
  MessageCircleOff,
  Sparkles,
  Download,
  Upload,
  CheckCircle,
  AlertCircle,
  Database,
  Check,
  Calendar,
  Bell,
  TrendingUp,
  CheckSquare
} from "lucide-react";
import { Lead, LeadActivity, LeadSource, LeadStatus, Task } from "../types";
import { SoundEngine } from "../utils/sound";

interface LeadManagementProps {
  leads: Lead[];
  onAddLead: (leadData: Partial<Lead>) => void;
  onUpdateLead: (leadId: string, updatedFields: Partial<Lead>) => void;
  onBulkAssignLeads?: (assignments: { leadId: string, assignedStaff: string }[]) => void;
  onAddActivity: (leadId: string, activityData: { actionType: string; details: string }) => void;
  activities: { [leadId: string]: LeadActivity[] };
  fetchActivities: (leadId: string) => void;
  onImportLeads?: (leadsArray: Partial<Lead>[]) => Promise<Lead[]>;
  onAddTask?: (taskData: Partial<Task>) => Promise<any>;
  tasks?: Task[];
  currentUser?: any;
  coworkers?: any[];
}

export function LeadManagement({
  leads,
  onAddLead,
  onUpdateLead,
  onBulkAssignLeads,
  onAddActivity,
  activities,
  fetchActivities,
  onImportLeads,
  onAddTask,
  tasks = [],
  currentUser,
  coworkers = []
}: LeadManagementProps) {
  // UI states
  const [isOpenAddModal, setIsOpenAddModal] = React.useState(false);
  const [isOpenImportModal, setIsOpenImportModal] = React.useState(false);
  const [parsedPreviewLeads, setParsedPreviewLeads] = React.useState<any[]>([]);
  const [importStatus, setImportStatus] = React.useState<"idle" | "parsing" | "success" | "error" | "uploading">("idle");
  const [importErrorMessage, setImportErrorMessage] = React.useState("");
  const [dragActive, setDragActive] = React.useState(false);

  const [selectedLead, setSelectedLead] = React.useState<Lead | null>(null);
  const [filterSource, setFilterSource] = React.useState<string>("All");
  const [filterLanguage, setFilterLanguage] = React.useState<string>("All");
  const [searchQuery, setSearchQuery] = React.useState<string>("");
  const [ownerFilter, setOwnerFilter] = React.useState<"all" | "personal">(currentUser ? "personal" : "all");
  const [selectedLeadIds, setSelectedLeadIds] = React.useState<string[]>([]);
  const [isBulkSelectMode, setIsBulkSelectMode] = React.useState(false);
  const [bulkAssignee, setBulkAssignee] = React.useState<string>("");

  const isAdmin = !currentUser || currentUser.role === "Super Admin" || currentUser.role === "Admin" || currentUser.id === "guest";

  // Lead Modal Entry State
  const [formData, setFormData] = React.useState({
    name: "",
    mobile: "",
    email: "",
    country: "India",
    city: "",
    language: "Tamil",
    source: "Manual Entry" as LeadSource,
    interestType: "Consultation" as "Consultation" | "Course" | "Workshop",
    notes: "",
    assignedStaff: currentUser ? currentUser.name : "Unassigned"
  });

  React.useEffect(() => {
    if (currentUser) {
      setFormData(prev => ({ ...prev, assignedStaff: currentUser.name }));
      // Non-admins are restricted permanently to personal view for strict CRM separation
      if (!isAdmin) {
        setOwnerFilter("personal");
      } else {
        setOwnerFilter("all");
      }
    } else {
      setFormData(prev => ({ ...prev, assignedStaff: "Unassigned" }));
      setOwnerFilter("all");
    }
  }, [currentUser, isAdmin]);

  // Call timeline Entry state
  const [callNote, setCallNote] = React.useState("");
  const [actionType, setActionType] = React.useState<"Call" | "WhatsApp" | "Email" | "Note Added">("Call");

  // Scheduling and Call Reminders Form States
  const [schedType, setSchedType] = React.useState<"Call" | "WhatsApp" | "Meeting" | "General">("Call");
  const [schedTime, setSchedTime] = React.useState("");
  const [schedNotes, setSchedNotes] = React.useState("");
  const [schedPriority, setSchedPriority] = React.useState<"High" | "Medium" | "Low">("Medium");

  const applyPresetTime = (minutes: number) => {
    const d = new Date();
    d.setMinutes(d.getMinutes() + minutes);
    const pad = (n: number) => n.toString().padStart(2, "0");
    const formatted = `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
    setSchedTime(formatted);
  };

  const applyTomorrowMorning = () => {
    const d = new Date();
    d.setDate(d.getDate() + 1);
    d.setHours(10, 0, 0, 0); 
    const pad = (n: number) => n.toString().padStart(2, "0");
    const formatted = `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
    setSchedTime(formatted);
  };

  const handleCreateReminderTask = () => {
    if (!selectedLead) return;
    if (!schedTime) {
      alert("Please select a date and time for the reminder.");
      return;
    }

    const scheduledDate = schedTime.split("T")[0];
    const parsedTime = new Date(schedTime).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });
    
    const taskTitle = `[${schedType} Reminder] Lead ${selectedLead.name}: ${schedNotes || `Follow-up call on ${schedType}`}`;
    
    const payload = {
      title: taskTitle,
      assignedTo: selectedLead.assignedStaff || "Unassigned",
      dueDate: scheduledDate,
      priority: schedPriority,
      leadId: selectedLead.id,
      isReminder: true,
      reminderTime: schedTime
    };

    if (onAddTask) {
      onAddTask(payload).then(() => {
        const logMsg = `[Reminder Scheduled] Follow-up ${schedType} task reminder created for ${scheduledDate} at ${parsedTime}. Note: ${schedNotes || "none"}`;
        onAddActivity(selectedLead.id, {
          actionType: "Note Added",
          details: logMsg
        });
        
        setSchedNotes("");
        setSchedTime("");
        setSchedType("Call");
        setSchedPriority("Medium");
      });
    } else {
      alert("Task Scheduler service is not configured on this component.");
    }
  };

  const sources: LeadSource[] = [
    "Website",
    "WhatsApp",
    "Instagram",
    "Facebook",
    "YouTube",
    "Google Forms",
    "Referral",
    "Manual Entry"
  ];

  const statuses: LeadStatus[] = [
    "New Lead",
    "Contacted",
    "Interested",
    "Follow-up Required",
    "Consultation Booked",
    "Course Interested",
    "Converted",
    "Lost"
  ];

  const languages = ["Tamil", "English", "Hindi", "Telugu", "Kannada", "Malayalam"];

  // Filtered Leads list
  const filteredLeads = leads.filter(l => {
    const matchesSource = filterSource === "All" || l.source === filterSource;
    const matchesLanguage = filterLanguage === "All" || l.language === filterLanguage;
    
    let matchesOwner = true;
    if (ownerFilter === "personal" && currentUser) {
      const staffNameLower = (l.assignedStaff || "").toLowerCase();
      const userNameLower = (currentUser.name || "").toLowerCase();
      // Match if equal, or if names overlap (e.g. "Ramanujan" overlap, or if assigned to them)
      matchesOwner = staffNameLower.includes(userNameLower) || userNameLower.includes(staffNameLower);
    }

    const matchesKeyword =
      l.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      l.mobile.includes(searchQuery) ||
      l.country.toLowerCase().includes(searchQuery.toLowerCase());
    return matchesSource && matchesLanguage && matchesOwner && matchesKeyword;
  });

  // Split and Distribute Leads (Traditional CRM round-robin tool)
  const handleRoundRobinSplit = () => {
    const unassigned = leads.filter(l => !l.assignedStaff || l.assignedStaff === "Unassigned" || l.assignedStaff === "");
    if (unassigned.length === 0) {
      alert("No vacant/unassigned leads exist in the CRM pool to split.");
      return;
    }
    // Coworkers that can receive leads: exclude Super Admin to distribute strictly among working staff
    let agents = coworkers.filter(c => c.role !== "Super Admin");
    if (agents.length === 0) {
      agents = coworkers; // Fallback to all if none match
    }
    if (agents.length === 0) {
      alert("Please configure coworker profiles in Employee lists first so they are ready to receive leads!");
      return;
    }

    const confirmSplit = window.confirm(`Are you sure you want to equally split and distribute ${unassigned.length} unassigned leads among your ${agents.length} coworkers? This will guarantee separate lists without overlap.`);
    if (!confirmSplit) return;

    const assignments = unassigned.map((lead, idx) => {
      const targetAgent = agents[idx % agents.length];
      return {
        leadId: lead.id,
        assignedStaff: targetAgent.name
      };
    });

    if (onBulkAssignLeads) {
      onBulkAssignLeads(assignments);
      SoundEngine.playSuccessAlert();
    }
  };

  // Assign manually selected leads
  const handleBulkAssignSelected = () => {
    if (selectedLeadIds.length === 0) {
      alert("Please select lead checkboxes on the cards first.");
      return;
    }
    if (!bulkAssignee || bulkAssignee === "") {
      alert("Please select a target coworker to receive this separate batch.");
      return;
    }

    const assignments = selectedLeadIds.map(leadId => ({
      leadId,
      assignedStaff: bulkAssignee
    }));

    if (onBulkAssignLeads) {
      onBulkAssignLeads(assignments);
      setSelectedLeadIds([]);
      setIsBulkSelectMode(false);
      SoundEngine.playSuccessAlert();
    }
  };

  const handleOpenLeadDetails = (lead: Lead) => {
    setSelectedLead(lead);
    fetchActivities(lead.id);
  };

  const submitAddLead = (e: React.FormEvent) => {
    e.preventDefault();
    if (!formData.name || !formData.mobile) {
      alert("Name and Mobile are structural prerequisite parameters");
      return;
    }
    onAddLead(formData);
    setIsOpenAddModal(false);
    // reset form
    setFormData({
      name: "",
      mobile: "",
      email: "",
      country: "India",
      city: "",
      language: "Tamil",
      source: "Manual Entry",
      interestType: "Consultation",
      notes: "",
      assignedStaff: "Astrologer Balakrishnan"
    });
  };

  // CSV Template and Import logic
  const handleCSVParse = (text: string) => {
    try {
      setImportStatus("parsing");
      const lines = text.split(/\r?\n/);
      if (lines.length < 2) {
        throw new Error("File seems to be empty or missing header lines. Make sure it contains header columns on the first line.");
      }

      const parseCSVLine = (line: string) => {
        const result = [];
        let curVal = "";
        let inQuotes = false;
        for (let i = 0; i < line.length; i++) {
          const char = line[i];
          if (char === '"') {
            inQuotes = !inQuotes;
          } else if (char === ',' && !inQuotes) {
            result.push(curVal.trim());
            curVal = "";
          } else {
            curVal += char;
          }
        }
        result.push(curVal.trim());
        return result;
      };

      const headers = parseCSVLine(lines[0]).map(h => h.toLowerCase().replace(/[\s_\-"]/g, ""));
      
      const expectedFields = {
        name: ["name", "leadname", "fullname", "clientname"],
        mobile: ["mobile", "phone", "contact", "mobilenumber", "phonenumber"],
        email: ["email", "emailaddress"],
        country: ["country", "nation"],
        city: ["city", "town"],
        language: ["language", "lang", "preferredlanguage"],
        source: ["source", "leadsource"],
        interestType: ["interest", "interesttype", "type"],
        notes: ["notes", "note", "message", "query"],
        assignedStaff: ["assignedstaff", "agent", "staff", "representative"]
      };

      const headerIndices: { [key: string]: number } = {};
      Object.keys(expectedFields).forEach(field => {
        const aliases = expectedFields[field as keyof typeof expectedFields];
        const index = headers.findIndex(h => aliases.includes(h));
        headerIndices[field] = index;
      });

      if (headerIndices.name === -1) headerIndices.name = 0;
      if (headerIndices.mobile === -1) {
        const phoneIdx = headers.findIndex(h => h.includes("phone") || h.includes("mobile") || h.includes("contact"));
        headerIndices.mobile = phoneIdx !== -1 ? phoneIdx : 1;
      }

      const parsedLeads: any[] = [];
      for (let i = 1; i < lines.length; i++) {
        const line = lines[i].trim();
        if (!line) continue;

        const cells = parseCSVLine(line);
        if (cells.length === 0 || (cells.length === 1 && !cells[0])) continue;

        const getColumnValue = (field: string, fallback = "") => {
          const idx = headerIndices[field];
          if (idx !== undefined && idx >= 0 && idx < cells.length) {
            let val = cells[idx];
            if (val.startsWith('"') && val.endsWith('"')) {
              val = val.substring(1, val.length - 1);
            }
            return val;
          }
          return fallback;
        };

        const nameValue = getColumnValue("name");
        const mobileValue = getColumnValue("mobile");
        
        // normalize interestType
        const normalizedInterest = getColumnValue("interestType", "Consultation").toLowerCase();
        let matchedInterest: "Consultation" | "Course" | "Workshop" | "Membership" | "Other" = "Consultation";
        if (normalizedInterest.includes("cour")) matchedInterest = "Course";
        else if (normalizedInterest.includes("work")) matchedInterest = "Workshop";
        else if (normalizedInterest.includes("memb")) matchedInterest = "Membership";
        else if (normalizedInterest.includes("oth")) matchedInterest = "Other";

        parsedLeads.push({
          name: nameValue || "",
          mobile: mobileValue || "",
          email: getColumnValue("email"),
          country: getColumnValue("country", "India"),
          city: getColumnValue("city"),
          language: getColumnValue("language", "Tamil"),
          source: getColumnValue("source", "Manual Entry"),
          interestType: matchedInterest,
          notes: getColumnValue("notes", "Bulk upload"),
          assignedStaff: getColumnValue("assignedStaff", "Unassigned"),
          isValid: !!nameValue && !!mobileValue,
          validationError: !nameValue ? "Missing Name" : !mobileValue ? "Missing Mobile" : ""
        });
      }

      if (parsedLeads.length === 0) {
        throw new Error("No readable records found in the uploaded file.");
      }

      setParsedPreviewLeads(parsedLeads);
      setImportStatus("idle");
    } catch (err: any) {
      setImportStatus("error");
      setImportErrorMessage(err.message || "Failed to parse file structure.");
    }
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      const file = e.target.files[0];
      const reader = new FileReader();
      reader.onload = (event) => {
        if (event.target && event.target.result) {
          handleCSVParse(event.target.result as string);
        }
      };
      reader.readAsText(file);
    }
  };

  const handleDrag = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    if (e.type === "dragenter" || e.type === "dragover") {
      setDragActive(true);
    } else if (e.type === "dragleave") {
      setDragActive(false);
    }
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setDragActive(false);
    if (e.dataTransfer.files && e.dataTransfer.files[0]) {
      const file = e.dataTransfer.files[0];
      const reader = new FileReader();
      reader.onload = (event) => {
        if (event.target && event.target.result) {
          handleCSVParse(event.target.result as string);
        }
      };
      reader.readAsText(file);
    }
  };

  const downloadCSVSampleTemplate = () => {
    const csvContent = 
      "Name,Mobile,Email,Country,City,Language,Source,Interest Type,Notes,Assigned Staff\n" +
      "Aditya Sen,9876543210,aditya.sen@gmail.com,India,Kolkatta,Bengali,Website,Course,Wants to join ALP Professional course,Dr. Muralidharan\n" +
      "Rajesh Kumar,9123456789,rajesh@whatsapp.com,India,Chennai,Tamil,WhatsApp,Consultation,Marriage horoscope matching evaluation needed,Archana S.\n" +
      "Meera Deshmukh,9345678120,meera.d@outlook.com,India,Mumbai,Marathi,Instagram,Workshop,Interested in upcoming Kundali workshop,Unassigned";

    const blob = new Blob([csvContent], { type: "text/csv;charset=utf-8;" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.setAttribute("download", "alp_leads_import_template.csv");
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  const executeBulkImport = () => {
    const validLeads = parsedPreviewLeads.filter(l => l.isValid);
    if (validLeads.length === 0) {
      alert("No valid lead structures found to import.");
      return;
    }

    if (onImportLeads) {
      setImportStatus("uploading");
      onImportLeads(validLeads)
        .then(() => {
          setImportStatus("success");
          setTimeout(() => {
            setIsOpenImportModal(false);
            setParsedPreviewLeads([]);
            setImportStatus("idle");
          }, 1500);
        })
        .catch(err => {
          setImportStatus("error");
          setImportErrorMessage(err.message || "Failed to commit import to backend CRM API.");
        });
    } else {
      validLeads.forEach(l => onAddLead(l));
      setImportStatus("success");
      setTimeout(() => {
        setIsOpenImportModal(false);
        setParsedPreviewLeads([]);
        setImportStatus("idle");
      }, 1500);
    }
  };

  const submitActivityNode = (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedLead || !callNote.trim()) return;

    onAddActivity(selectedLead.id, {
      actionType,
      details: callNote
    });

    setCallNote("");
    // Refresh timeline list on spot
    setTimeout(() => {
      fetchActivities(selectedLead.id);
    }, 100);
  };

  // WhatsApp automatic trigger template mocker
  const triggerWhatsAppTemplate = (templateName: string) => {
    if (!selectedLead) return;
    let messageText = "";
    if (templateName === "welcome") {
      messageText = `Pranams ${selectedLead.name}, thank you for contacting Akshaya Lagna Paddhati (ALP) Astrology. Our registered advisor will schedule your consultation shortly.`;
    } else {
      messageText = `Dear ${selectedLead.name}, your ALP Astrology course enrollment slot is reserved. Syllabus details has been dispatched. Please review schedules.`;
    }

    onAddActivity(selectedLead.id, {
      actionType: "WhatsApp",
      details: `[INTEGRATED OUTBOUND BOT] Sent templates: [${templateName}]. Text content: "${messageText}"`
    });

    alert(`Outbound WhatsApp template API fired successfully to ${selectedLead.mobile}! Logs updated on timeline.`);
    setTimeout(() => {
      fetchActivities(selectedLead.id);
    }, 100);
  };

  // Shift column manually
  const shiftLeadStatus = (leadId: string, currentStatus: LeadStatus, direction: "next" | "prev") => {
    const idx = statuses.indexOf(currentStatus);
    let newIdx = idx;
    if (direction === "next" && idx < statuses.length - 1) newIdx++;
    if (direction === "prev" && idx > 0) newIdx--;

    if (newIdx !== idx) {
      const nextStatus = statuses[newIdx];
      onUpdateLead(leadId, { status: nextStatus });
      // If we are updating currently selected lead, refresh view state
      if (selectedLead && selectedLead.id === leadId) {
        setSelectedLead(prev => prev ? { ...prev, status: nextStatus } : null);
        setTimeout(() => {
          fetchActivities(leadId);
        }, 100);
      }
    }
  };

  return (
    <div className="space-y-6">
      {/* Traditional CRM Lead Routing & Segment Split Center */}
      {currentUser && (
        <div className="bg-slate-900 border border-slate-800 p-4 rounded-2xl text-white space-y-4 shadow-xl">
          <div className="flex flex-col lg:flex-row justify-between items-start lg:items-center gap-4">
            <div className="space-y-1">
              <h3 className="text-xs font-black text-amber-400 uppercase tracking-widest flex items-center gap-2">
                <span className="w-2.5 h-2.5 rounded-full bg-emerald-500 animate-pulse" />
                {isAdmin ? "🏛️ CRM Central Allocation & Enterprise Routing Console" : "🔒 CRM Private Assigned Workplace"}
              </h3>
              <p className="text-[11px] text-slate-300 leading-relaxed font-medium">
                {isAdmin 
                  ? "Distribute incoming unassigned leads to your counselors using automatic split mechanisms or custom multi-selection rules." 
                  : `Showing strictly leads distributed directly to you (${currentUser.name}) by Admin. Contact logs are fully segregated.`}
              </p>
            </div>

            {isAdmin ? (
              <div className="bg-slate-950 p-1 rounded-xl inline-flex items-center gap-1 border border-slate-800">
                <button
                  type="button"
                  onClick={() => {
                    setOwnerFilter("personal");
                    SoundEngine.playChime();
                  }}
                  className={`px-3 py-1.5 text-[10px] font-black rounded-lg flex items-center gap-1.5 transition-all cursor-pointer ${
                    ownerFilter === "personal"
                      ? "bg-amber-500 text-slate-950"
                      : "text-slate-400 hover:text-white"
                  }`}
                >
                  <User className="w-3 h-3" />
                  <span>My Assigned</span>
                  <span className="bg-slate-950/20 text-slate-900 text-[9px] px-1.5 py-0.5 rounded-full font-mono font-bold">
                    {leads.filter(l => {
                      const staffNameLower = (l.assignedStaff || "").toLowerCase();
                      const userNameLower = (currentUser?.name || "").toLowerCase();
                      return staffNameLower.includes(userNameLower) || userNameLower.includes(staffNameLower);
                    }).length}
                  </span>
                </button>

                <button
                  type="button"
                  onClick={() => {
                    setOwnerFilter("all");
                    SoundEngine.playChime();
                  }}
                  className={`px-3 py-1.5 text-[10px] font-black rounded-lg flex items-center gap-1.5 transition-all cursor-pointer ${
                    ownerFilter === "all"
                      ? "bg-white text-slate-950"
                      : "text-slate-400 hover:text-white"
                  }`}
                >
                  <Database className="w-3 h-3" />
                  <span>All Enterprise Leads</span>
                  <span className="bg-slate-200 text-slate-900 text-[9px] px-1.5 py-0.5 rounded-full font-mono font-bold">
                    {leads.length}
                  </span>
                </button>
              </div>
            ) : (
              <div className="bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 px-3.5 py-1.5 rounded-xl text-[10.5px] font-mono tracking-tight font-black flex items-center gap-2">
                <span>🟢 Separated Pipeline Active: </span>
                <span className="bg-emerald-500 text-slate-950 px-2 py-0.5 rounded-md text-[10px] text-black">
                  {leads.filter(l => {
                    const staffNameLower = (l.assignedStaff || "").toLowerCase();
                    const userNameLower = (currentUser?.name || "").toLowerCase();
                    return staffNameLower.includes(userNameLower) || userNameLower.includes(staffNameLower);
                  }).length} Leads Assigned
                </span>
              </div>
            )}
          </div>

          {/* Admin Distribution Mechanics Panel */}
          {isAdmin && (
            <div className="pt-3.5 border-t border-slate-800 grid grid-cols-1 md:flex md:flex-wrap items-center gap-4 text-xs">
              <div className="text-slate-350 font-mono text-[10px] uppercase tracking-wider font-extrabold flex items-center gap-2">
                <span>⚡ Allocation Toolkit:</span>
                <span className="bg-slate-800 text-slate-100 px-2.5 py-1 rounded font-normal font-sans tracking-normal capitalize">
                  {leads.filter(l => !l.assignedStaff || l.assignedStaff === "Unassigned" || l.assignedStaff === "").length} Unassigned Leads Remaining
                </span>
              </div>

              <div className="flex flex-wrap items-center gap-2.5 md:ml-auto">
                {/* Round Robin Split */}
                <button
                  type="button"
                  onClick={handleRoundRobinSplit}
                  className="bg-amber-500 hover:bg-amber-450 text-slate-950 font-black px-3.5 py-2 rounded-xl cursor-pointer flex items-center gap-1.5 text-[11px] uppercase tracking-wide transition-colors"
                >
                  <TrendingUp className="w-3.5 h-3.5 text-slate-950" />
                  <span>Equal Round Robin Auto-Split</span>
                </button>

                {/* Manual Split selector toggle */}
                <button
                  type="button"
                  onClick={() => {
                    setIsBulkSelectMode(!isBulkSelectMode);
                    setSelectedLeadIds([]);
                    SoundEngine.playChime();
                  }}
                  className={`px-3.5 py-2 rounded-xl font-bold flex items-center gap-1.5 text-[11px] transition-all cursor-pointer border ${
                    isBulkSelectMode 
                      ? "bg-rose-500 text-white border-rose-500" 
                      : "bg-slate-800 hover:bg-slate-755 text-slate-200 border-slate-700"
                  }`}
                >
                  <CheckSquare className="w-3.5 h-3.5" />
                  <span>{isBulkSelectMode ? "Cancel Selector" : "Custom Multi-Check selector"}</span>
                </button>

                {/* Bulk assign triggers */}
                {isBulkSelectMode && (
                  <div className="flex items-center gap-2 bg-slate-950 p-1.5 rounded-xl border border-slate-800 animate-in fade-in duration-200">
                    <span className="text-[10px] font-mono text-slate-400 pl-1.5 font-bold">
                      {selectedLeadIds.length} Selected →
                    </span>
                    <select
                      value={bulkAssignee}
                      onChange={(e) => setBulkAssignee(e.target.value)}
                      className="bg-slate-900 text-white border border-slate-800 rounded-lg px-2 py-1 text-[11px] cursor-pointer max-w-[130px] focus:outline-none"
                    >
                      <option value="">Select advisor...</option>
                      <option value="Unassigned">Unassigned</option>
                      {coworkers.filter(c => c.name !== currentUser?.name).map(cw => (
                        <option key={cw.id} value={cw.name}>{cw.name}</option>
                      ))}
                    </select>

                    <button
                      type="button"
                      disabled={selectedLeadIds.length === 0 || !bulkAssignee}
                      onClick={handleBulkAssignSelected}
                      className="bg-emerald-500 hover:bg-emerald-450 text-slate-950 font-extrabold px-3 py-1.5 text-[10px] rounded-lg transition-colors cursor-pointer disabled:opacity-40 disabled:cursor-not-allowed"
                    >
                      Assign Split
                    </button>
                  </div>
                )}
              </div>
            </div>
          )}
        </div>
      )}

      {/* Search and Filters Dashboard Control Strip */}
      <div className="flex flex-col xl:flex-row gap-4 justify-between items-start xl:items-center">
        <div className="flex flex-wrap items-center gap-3 w-full xl:w-auto">
          {/* Quick Search */}
          <div className="relative flex-1 sm:max-w-xs min-w-44">
            <input
              type="text"
              placeholder="Search leads by name or phone..."
              value={searchQuery}
              onChange={e => setSearchQuery(e.target.value)}
              className="w-full bg-white border border-slate-300 text-xs rounded-lg px-3 py-2 text-slate-800 focus:outline-none focus:border-amber-500 transition-colors shadow-xs"
            />
          </div>

          {/* Source filters dropdown */}
          <div className="flex items-center gap-2 bg-white border border-slate-200 px-3 py-1.5 rounded-lg text-xs shadow-xs">
            <Filter className="w-3.5 h-3.5 text-amber-600 shrink-0" />
            <select
              value={filterSource}
              onChange={e => setFilterSource(e.target.value)}
              className="bg-transparent text-slate-700 focus:outline-none border-none text-xs cursor-pointer"
            >
              <option value="All">All Sources</option>
              {sources.map(src => (
                <option key={src} value={src}>{src}</option>
              ))}
            </select>
          </div>

          {/* Preferred Language dropdown */}
          <div className="flex items-center gap-2 bg-white border border-slate-200 px-3 py-1.5 rounded-lg text-xs shadow-xs">
            <User className="w-3.5 h-3.5 text-amber-600 shrink-0" />
            <select
              value={filterLanguage}
              onChange={e => setFilterLanguage(e.target.value)}
              className="bg-transparent text-slate-700 focus:outline-none border-none text-xs cursor-pointer"
            >
              <option value="All">All Languages</option>
              {languages.map(lang => (
                <option key={lang} value={lang}>{lang}</option>
              ))}
            </select>
          </div>
        </div>

        {/* Actions Button Row */}
        <div className="flex flex-wrap items-center gap-2 w-full sm:w-auto self-stretch sm:self-auto">
          {/* Download CSV template */}
          <button
            onClick={downloadCSVSampleTemplate}
            type="button"
            className="border border-slate-300 hover:bg-slate-100 text-slate-705 font-bold px-3 py-2 text-xs rounded-lg flex items-center gap-1.5 shadow-xs cursor-pointer justify-center flex-1 sm:flex-none"
            title="Download CSV sample file format"
          >
            <Download className="w-3.5 h-3.5 text-slate-500" />
            <span>Download CSV Template</span>
          </button>
          
          {/* Import CSV trigger */}
          <button
            onClick={() => setIsOpenImportModal(true)}
            type="button"
            className="border border-slate-300 bg-slate-50 hover:bg-slate-100 text-slate-705 font-bold px-3 py-2 text-xs rounded-lg flex items-center gap-1.5 shadow-xs cursor-pointer justify-center flex-1 sm:flex-none"
          >
            <Upload className="w-3.5 h-3.5 text-amber-600" />
            <span>Import Leads</span>
          </button>

          {/* Add Lead Trigger */}
          <button
            onClick={() => setIsOpenAddModal(true)}
            className="bg-amber-500 hover:bg-amber-600 text-slate-950 font-bold px-4 py-2 text-xs rounded-lg flex items-center gap-1.5 shadow-sm cursor-pointer justify-center flex-1 sm:flex-none"
          >
            <Plus className="w-4 h-4" />
            <span>Generate Lead Capture</span>
          </button>
        </div>
      </div>

      {filteredLeads.length === 0 && (
        <div className="bg-amber-500/10 border border-amber-550/15 text-slate-800 p-6 rounded-2xl text-center space-y-3 max-w-lg mx-auto my-4 animate-in fade-in duration-350">
          <AlertCircle className="w-8 h-8 text-amber-600 mx-auto" />
          <h4 className="font-extrabold uppercase text-xs text-amber-900 tracking-tight">No Leads Found For This Separate Workspace</h4>
          <p className="text-xs text-slate-650 leading-relaxed">
            There are currently no leads assigned directly to your co-worker profile (<strong className="text-slate-800">{currentUser?.name || "Viewer"}</strong>) matching the active criteria. Toggle to <strong className="text-slate-800">All Enterprise Leads</strong> above to claim an existing log or add a new capture to your session!
          </p>
          {ownerFilter === "personal" && (
            <button
              onClick={() => {
                setOwnerFilter("all");
                SoundEngine.playChime();
              }}
              className="text-[10px] font-black uppercase tracking-widest bg-slate-900 text-slate-100 px-4 py-2 rounded-xl cursor-pointer hover:bg-slate-800 transition-colors"
            >
              🚀 Show All CRM Prospects
            </button>
          )}
        </div>
      )}

      {/* Kanban Board Layout */}
      <div className="overflow-x-auto pb-4">
        <div className="flex gap-4 min-w-[1200px]">
          {statuses.map(colStatus => {
            const columnLeads = filteredLeads.filter(l => l.status === colStatus);
            return (
              <div key={colStatus} className="w-80 bg-slate-100/60 border border-slate-200 rounded-xl p-3 flex flex-col h-[520px] shrink-0">
                {/* Column Header */}
                <div className="flex justify-between items-center mb-3 border-b border-slate-200 pb-2 shrink-0">
                  <span className="text-[11px] font-bold uppercase tracking-wider text-slate-700 font-sans">
                    {colStatus}
                  </span>
                  <span className="text-[10px] bg-white border border-slate-200 text-slate-500 font-mono px-2 py-0.5 rounded-full">
                    {columnLeads.length}
                  </span>
                </div>

                {/* Ticket items frame */}
                <div className="flex-1 overflow-y-auto space-y-2 pr-1">
                  {columnLeads.map(lead => {
                    const isSelected = selectedLeadIds.includes(lead.id);
                    return (
                      <div
                        key={lead.id}
                        onClick={() => {
                          if (isBulkSelectMode && isAdmin) {
                            if (isSelected) {
                              setSelectedLeadIds(prev => prev.filter(id => id !== lead.id));
                            } else {
                              setSelectedLeadIds(prev => [...prev, lead.id]);
                            }
                            SoundEngine.playChime();
                          } else {
                            handleOpenLeadDetails(lead);
                          }
                        }}
                        className={`border rounded-lg p-3 space-y-2.5 transition-all shadow-xs cursor-pointer relative group text-xs text-slate-800 ${
                          isSelected && isBulkSelectMode && isAdmin
                            ? "bg-amber-500/10 border-amber-500 ring-2 ring-amber-500/20"
                            : "bg-white border-slate-200 hover:border-amber-500/30"
                        }`}
                      >
                        {/* Badge source flag */}
                        <div className="flex justify-between items-center">
                          <div className="flex items-center gap-1.5">
                            {isBulkSelectMode && isAdmin && (
                              <input
                                type="checkbox"
                                checked={isSelected}
                                readOnly
                                className="w-3.5 h-3.5 text-amber-500 border-slate-300 rounded focus:ring-amber-500 shrink-0 cursor-pointer"
                              />
                            )}
                            <span className="text-[9px] font-mono font-bold uppercase bg-amber-500/10 text-amber-800 px-2 py-0.5 rounded-xs animate-pulse">
                              {lead.source}
                            </span>
                          </div>
                          {!isBulkSelectMode && (
                            <div className="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
                              <button
                                title="Promote Status"
                                onClick={(e) => {
                                  e.stopPropagation();
                                  shiftLeadStatus(lead.id, lead.status, "next");
                                }}
                                className="p-1 rounded bg-slate-100 hover:bg-slate-200 text-slate-700 border border-slate-200"
                              >
                                →
                              </button>
                            </div>
                          )}
                        </div>

                      {/* Header context */}
                      <div className="space-y-0.5">
                        <h4 className="font-semibold text-slate-800">{lead.name}</h4>
                        <p className="text-[10px] text-slate-500 truncate">{lead.mobile} | {lead.country}</p>
                      </div>

                      <div className="flex flex-wrap gap-1">
                        <span className="text-[9px] bg-slate-100 border border-slate-200 text-slate-600 font-mono px-1.5 py-0.5 rounded-xs">
                          Lang: {lead.language}
                        </span>
                        <span className="text-[9px] bg-amber-500/10 text-amber-700 font-mono px-1.5 py-0.5 rounded-xs font-semibold">
                          {lead.interestType}
                        </span>
                      </div>

                      {/* notes slice summary */}
                      {lead.notes && (
                        <p className="text-[10px] text-slate-500 line-clamp-2 leading-relaxed bg-slate-50 p-2 rounded border border-slate-150">
                          {lead.notes}
                        </p>
                      )}

                      {/* Footer assign status */}
                      <div className="border-t border-slate-100 pt-2 flex justify-between items-center text-[10px] text-slate-500">
                        {currentUser && (lead.assignedStaff || "").toLowerCase().includes((currentUser.name || "").toLowerCase()) ? (
                          <span className="truncate text-emerald-700 font-extrabold flex items-center gap-1 bg-emerald-500/10 px-1.5 py-0.5 rounded-sm">
                            👤 Rep: You ({currentUser.name})
                          </span>
                        ) : (
                          <span className="truncate text-slate-600 font-medium bg-slate-100 px-1.5 py-0.5 rounded-sm">
                            👥 Rep: {lead.assignedStaff || "Unassigned"}
                          </span>
                        )}
                      </div>
                    </div>
                    );
                  })}

                  {columnLeads.length === 0 && (
                    <div className="text-center py-12 text-slate-400 text-[10px] border border-dashed border-slate-200 rounded-lg flex flex-col justify-center items-center">
                      Empty column list
                    </div>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {/* LEAD QUICK-EDIT AND TIMELINE DIALOG MODAL */}
      {selectedLead && (
        <div className="fixed inset-0 bg-slate-950/80 backdrop-blur-xs flex items-center justify-center z-50 p-4 animate-fade-in">
          <div className="bg-slate-900 border border-amber-500/15 rounded-xl w-full max-w-4xl max-h-[90vh] flex flex-col overflow-hidden text-xs">
            {/* Modal Header */}
            <div className="p-4 bg-slate-950/60 border-b border-light/5 flex justify-between items-center shrink-0">
              <div className="flex items-center gap-2">
                <span className="text-[9px] font-mono text-amber-400 bg-amber-500/10 border border-amber-500/20 px-2 py-0.5 rounded-sm font-bold uppercase">
                  {selectedLead.status}
                </span>
                <h3 className="font-bold text-slate-100 text-sm">Lead details: {selectedLead.name}</h3>
              </div>
              <button
                onClick={() => setSelectedLead(null)}
                className="p-1 rounded-md text-slate-400 hover:text-white hover:bg-white/5 cursor-pointer"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Split Modal Body */}
            <div className="flex-1 overflow-y-auto grid grid-cols-1 md:grid-cols-2">
              {/* Left Column - Core Fields Data Editor Form */}
              <div className="p-5 border-r border-white/5 space-y-4">
                <h4 className="font-bold text-amber-400 uppercase tracking-widest text-[10px] font-mono mb-2">
                  Prospect Details Profile
                </h4>
                <div className="grid grid-cols-2 gap-3.5">
                  <div>
                    <label className="text-[10px] font-mono text-slate-400 block mb-1">Mobile Contact</label>
                    <input
                      type="text"
                      value={selectedLead.mobile}
                      onChange={e => onUpdateLead(selectedLead.id, { mobile: e.target.value })}
                      className="w-full bg-slate-950 border border-slate-800 rounded px-2.5 py-1.5 text-white text-xs"
                    />
                  </div>
                  <div>
                    <label className="text-[10px] font-mono text-slate-400 block mb-1">Primary Email</label>
                    <input
                      type="email"
                      value={selectedLead.email}
                      onChange={e => onUpdateLead(selectedLead.id, { email: e.target.value })}
                      className="w-full bg-slate-950 border border-slate-800 rounded px-2.5 py-1.5 text-white text-xs"
                    />
                  </div>
                  <div>
                    <label className="text-[10px] font-mono text-slate-400 block mb-1">Country</label>
                    <input
                      type="text"
                      value={selectedLead.country}
                      onChange={e => onUpdateLead(selectedLead.id, { country: e.target.value })}
                      className="w-full bg-slate-950 border border-slate-800 rounded px-2.5 py-1.5 text-white text-xs"
                    />
                  </div>
                  <div>
                    <label className="text-[10px] font-mono text-slate-400 block mb-1">Primary Language</label>
                    <select
                      value={selectedLead.language}
                      onChange={e => onUpdateLead(selectedLead.id, { language: e.target.value })}
                      className="w-full bg-slate-950 border border-slate-800 rounded px-2 py-1.5 text-white text-xs cursor-pointer"
                    >
                      {languages.map(l => (
                        <option key={l} value={l} className="bg-slate-900">{l}</option>
                      ))}
                    </select>
                  </div>
                  <div>
                    <div className="flex justify-between items-center mb-1">
                      <label className="text-[10px] font-mono text-slate-400 block">Staff Owner Assigned</label>
                      {currentUser && selectedLead.assignedStaff !== currentUser.name && (
                        <button
                          type="button"
                          onClick={() => {
                            onUpdateLead(selectedLead.id, { assignedStaff: currentUser.name });
                            setSelectedLead(prev => prev ? { ...prev, assignedStaff: currentUser.name } : null);
                            SoundEngine.playChime();
                          }}
                          className="text-[8px] font-black text-amber-500 hover:text-amber-450 transition-colors uppercase font-mono bg-amber-500/10 px-1.5 py-0.5 rounded border border-amber-500/30 cursor-pointer"
                        >
                          ☝ Claim
                        </button>
                      )}
                    </div>
                    {coworkers && coworkers.length > 0 ? (
                      <select
                        value={selectedLead.assignedStaff}
                        onChange={e => {
                          onUpdateLead(selectedLead.id, { assignedStaff: e.target.value });
                          setSelectedLead(prev => prev ? { ...prev, assignedStaff: e.target.value } : null);
                        }}
                        className="w-full bg-slate-950 border border-slate-800 rounded px-2.5 py-1.5 text-white text-xs cursor-pointer"
                      >
                        <option value="Unassigned">Unassigned</option>
                        {coworkers.map(c => (
                          <option key={c.id} value={c.name} className="bg-slate-900">{c.name} ({c.role})</option>
                        ))}
                        {selectedLead.assignedStaff && !coworkers.some(c => c.name === selectedLead.assignedStaff) && (
                          <option value={selectedLead.assignedStaff} className="bg-slate-900">{selectedLead.assignedStaff}</option>
                        )}
                      </select>
                    ) : (
                      <input
                        type="text"
                        value={selectedLead.assignedStaff}
                        onChange={e => {
                          onUpdateLead(selectedLead.id, { assignedStaff: e.target.value });
                          setSelectedLead(prev => prev ? { ...prev, assignedStaff: e.target.value } : null);
                        }}
                        className="w-full bg-slate-950 border border-slate-800 rounded px-2.5 py-1.5 text-white text-xs"
                      />
                    )}
                  </div>
                  <div>
                    <label className="text-[10px] font-mono text-slate-400 block mb-1">Shift Funnel Pipeline</label>
                    <div className="flex gap-1.5">
                      <button
                        onClick={() => shiftLeadStatus(selectedLead.id, selectedLead.status, "prev")}
                        className="flex-1 bg-slate-800 hover:bg-slate-755 border border-slate-700 py-1 rounded text-center text-slate-200 font-bold"
                      >
                        ◀ Back
                      </button>
                      <button
                        onClick={() => shiftLeadStatus(selectedLead.id, selectedLead.status, "next")}
                        className="flex-1 bg-slate-800 hover:bg-slate-755 border border-slate-700 py-1 rounded text-center text-slate-200 font-bold"
                      >
                        Forward ▶
                      </button>
                    </div>
                  </div>
                </div>

                {/* CRM Notes block */}
                <div>
                  <label className="text-[10px] font-mono text-slate-400 block mb-1">Initial Requirement Logs</label>
                  <textarea
                    rows={3}
                    value={selectedLead.notes}
                    onChange={e => onUpdateLead(selectedLead.id, { notes: e.target.value })}
                    className="w-full bg-slate-950 border border-slate-800 rounded p-2.5 text-white text-xs"
                  />
                </div>

                {/* WhatsApp Trigger Panel */}
                <div className="bg-slate-950/40 border border-white/5 rounded-lg p-3.5 space-y-2">
                  <span className="text-[10px] font-bold font-mono tracking-wider text-amber-400 flex items-center gap-1.5">
                    <MessageSquare className="w-3.5 h-3.5" />
                    <span>WhatsApp Broadcaster Triggers</span>
                  </span>
                  <p className="text-[10px] text-slate-500 leading-normal">
                    Fires automatic outbox messaging flow to client contact coordinates in compliance with standard templates registers.
                  </p>
                  <div className="flex gap-2">
                    <button
                      onClick={() => triggerWhatsAppTemplate("welcome")}
                      className="flex-1 bg-linear-to-r from-emerald-600 to-teal-600 hover:from-emerald-500 hover:to-teal-500 text-slate-950 font-bold py-1.5 rounded text-[10px] transition-all cursor-pointer"
                    >
                      Welcome Template
                    </button>
                    <button
                      onClick={() => triggerWhatsAppTemplate("academy")}
                      className="flex-1 bg-linear-to-r from-emerald-600 to-teal-600 hover:from-emerald-500 hover:to-teal-500 text-slate-950 font-bold py-1.5 rounded text-[10px] transition-all cursor-pointer"
                    >
                      Academy Pitch Template
                    </button>
                  </div>
                </div>

                {/* Schedule & Call Reminder Panel */}
                <div className="bg-slate-950/40 border border-white/5 rounded-lg p-3.5 space-y-3">
                  <span className="text-[10px] font-bold font-mono tracking-wider text-amber-400 flex items-center gap-1.5">
                    <Bell className="w-3.5 h-3.5 text-amber-500 animate-pulse" />
                    <span>Schedule / Reminders & Calls</span>
                  </span>
                  
                  <div className="space-y-2">
                    {/* Presets */}
                    <div className="space-y-1">
                      <label className="text-[9px] text-slate-400 font-mono block">Quick Time Presets</label>
                      <div className="flex flex-wrap gap-1">
                        <button
                          type="button"
                          onClick={() => applyPresetTime(30)}
                          className="bg-slate-800 hover:bg-slate-700 text-slate-300 text-[9px] font-semibold px-2 py-1 rounded transition-colors cursor-pointer"
                        >
                          +30 Min
                        </button>
                        <button
                          type="button"
                          onClick={() => applyPresetTime(120)}
                          className="bg-slate-800 hover:bg-slate-700 text-slate-300 text-[9px] font-semibold px-2 py-1 rounded transition-colors cursor-pointer"
                        >
                          +2 Hrs
                        </button>
                        <button
                          type="button"
                          onClick={() => applyTomorrowMorning()}
                          className="bg-slate-800 hover:bg-slate-700 text-slate-300 text-[9px] font-semibold px-2 py-1 rounded transition-colors cursor-pointer"
                        >
                          Tomorrow 10 AM
                        </button>
                      </div>
                    </div>

                    {/* Inputs */}
                    <div className="grid grid-cols-2 gap-2">
                      <div>
                        <label className="text-[9px] text-slate-400 font-mono block mb-1">Alert Type</label>
                        <select
                          value={schedType}
                          onChange={(e) => setSchedType(e.target.value as any)}
                          className="w-full bg-slate-950 border border-slate-800 rounded px-2 py-1 text-white text-[10px] cursor-pointer"
                        >
                          <option value="Call">Call Reminder</option>
                          <option value="WhatsApp">WhatsApp Alert</option>
                          <option value="Meeting">Meeting Session</option>
                          <option value="General">General Task</option>
                        </select>
                      </div>
                      <div>
                        <label className="text-[9px] text-slate-400 font-mono block mb-1">Priority</label>
                        <select
                          value={schedPriority}
                          onChange={(e) => setSchedPriority(e.target.value as any)}
                          className="w-full bg-slate-950 border border-slate-800 rounded px-2 py-1 text-white text-[10px] cursor-pointer"
                        >
                          <option value="Low">Low</option>
                          <option value="Medium">Medium</option>
                          <option value="High">High</option>
                        </select>
                      </div>
                    </div>

                    <div>
                      <label className="text-[9px] text-slate-400 font-mono block mb-1">Schedule Date & Time</label>
                      <input
                        type="datetime-local"
                        value={schedTime}
                        onChange={(e) => setSchedTime(e.target.value)}
                        className="w-full bg-slate-950 border border-slate-800 rounded px-2 py-1 text-white text-[10px]"
                      />
                    </div>

                    <div>
                      <label className="text-[9px] text-slate-400 font-mono block mb-1">Short Context Notes</label>
                      <input
                        type="text"
                        placeholder="e.g. Call back re: fee waivers, horoscope validation..."
                        value={schedNotes}
                        onChange={(e) => setSchedNotes(e.target.value)}
                        className="w-full bg-slate-955 border border-slate-800 rounded px-2 py-1 text-white text-[10px]"
                      />
                    </div>

                    <button
                      type="button"
                      onClick={handleCreateReminderTask}
                      className="w-full bg-amber-500 hover:bg-amber-600 text-slate-950 font-bold py-1.5 rounded text-[10px] transition-colors cursor-pointer flex items-center justify-center gap-1.5 shadow-xs"
                    >
                      <Calendar className="w-3.5 h-3.5" />
                      <span>Process & Save Calling Schedule</span>
                    </button>
                  </div>

                  {/* Existing reminders */}
                  {(() => {
                    const activeReminders = tasks.filter(t => t.leadId === selectedLead.id);
                    if (activeReminders.length > 0) {
                      return (
                        <div className="border-t border-white/5 pt-2.5 mt-1 space-y-1.5">
                          <label className="text-[9px] text-amber-500/80 font-mono block font-bold uppercase tracking-wider">
                            Planned Follow-Ups & Reminders ({activeReminders.length})
                          </label>
                          <div className="space-y-1 max-h-[140px] overflow-y-auto pr-1">
                            {activeReminders.map(rem => {
                              const isCompleted = rem.status === "Completed";
                              return (
                                <div key={rem.id} className={`p-2 rounded border border-white/5 bg-slate-950/60 flex justify-between items-center gap-1.5 ${isCompleted ? "opacity-55" : ""}`}>
                                  <div className="space-y-0.5 truncate flex-1">
                                    <div className="flex items-center gap-1">
                                      <span className={`text-[8px] px-1 rounded-sm font-mono font-bold uppercase ${
                                        rem.priority === "High" ? "bg-rose-500/10 text-rose-450" : "bg-amber-500/10 text-amber-450"
                                      }`}>
                                        {rem.priority}
                                      </span>
                                      <span className="text-[9px] text-slate-350 font-semibold font-mono">
                                        Due: {rem.dueDate} {rem.reminderTime ? `at ${new Date(rem.reminderTime).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}` : ""}
                                      </span>
                                    </div>
                                    <p className={`text-[9.5px] text-slate-400 truncate ${isCompleted ? "line-through text-slate-500" : ""}`} title={rem.title}>
                                      {rem.title}
                                    </p>
                                  </div>
                                  <span className={`text-[8.5px] px-1.5 py-0.5 rounded-sm font-bold font-mono ${
                                    isCompleted ? "bg-emerald-500/10 text-emerald-400" : "bg-amber-500/10 text-amber-400"
                                  }`}>
                                    {rem.status}
                                  </span>
                                </div>
                              );
                            })}
                          </div>
                        </div>
                      );
                    }
                    return null;
                  })()}
                </div>
              </div>

              {/* Right Column - Timeline Tracker & Call Entry */}
              <div className="p-5 flex flex-col h-full overflow-hidden">
                <h4 className="font-bold text-amber-400 uppercase tracking-widest text-[10px] font-mono mb-3 shrink-0">
                  Lead Timeline Activity Trail
                </h4>

                {/* Submitting New Note timeline */}
                <form onSubmit={submitActivityNode} className="space-y-2 shrink-0 mb-4 bg-slate-950/30 p-3 rounded-lg border border-white/5">
                  <div className="flex justify-between items-center gap-2">
                    <span className="text-[9px] uppercase font-mono tracking-wider font-bold text-slate-400">
                      Record Log Activity Type
                    </span>
                    <div className="flex gap-1.5 text-[10px]">
                      {["Call", "WhatsApp", "Note Added"].map((tType) => (
                        <button
                          key={tType}
                          type="button"
                          onClick={() => setActionType(tType as any)}
                          className={`px-2 py-0.5 rounded ${
                            actionType === tType
                              ? "bg-amber-500 text-slate-950 font-bold"
                              : "bg-slate-800 text-slate-400 hover:text-white"
                          }`}
                        >
                          {tType}
                        </button>
                      ))}
                    </div>
                  </div>
                  <textarea
                    rows={2}
                    placeholder="Input logs or call summary description..."
                    value={callNote}
                    onChange={e => setCallNote(e.target.value)}
                    className="w-full bg-slate-955 border border-slate-800 rounded p-2 text-white text-xs focus:outline-none"
                  />
                  <div className="flex justify-end">
                    <button
                      type="submit"
                      className="bg-amber-500 hover:bg-amber-450 text-slate-950 font-bold px-3 py-1 rounded text-[10px] transition-all cursor-pointer"
                    >
                      Log Timeline Node
                    </button>
                  </div>
                </form>

                {/* Activity Feed rendering */}
                <div className="flex-1 overflow-y-auto space-y-3.5 pr-1 max-h-[220px]">
                  {(activities[selectedLead.id] || []).map((act, aIdx) => (
                    <div key={act.id || aIdx} className="border-l-2 border-amber-500/20 pl-3.5 space-y-1 relative">
                      <div className="absolute w-2 h-2 rounded-full bg-amber-500 -left-[5px] top-1"></div>
                      <div className="flex justify-between text-[10px]">
                        <span className="font-semibold text-slate-300 font-mono">
                          {act.actionType} • by {act.staffName}
                        </span>
                        <span className="text-slate-500">
                          {new Date(act.timestamp).toLocaleString([], { dateStyle: "short", timeStyle: "short" })}
                        </span>
                      </div>
                      <p className="text-[11px] text-slate-400 leading-relaxed font-sans bg-slate-950/15 p-2 rounded border border-white/5">
                        {act.details}
                      </p>
                    </div>
                  ))}

                  {(!activities[selectedLead.id] || activities[selectedLead.id].length === 0) && (
                    <div className="text-center py-6 text-slate-500 text-[10px]">
                      Loading timeline trail...
                    </div>
                  )}
                </div>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* LEAD CREATOR CAPTURE DIALOG */}
      {isOpenAddModal && (
        <div className="fixed inset-0 bg-slate-950/80 backdrop-blur-xs flex items-center justify-center z-50 p-4">
          <div className="bg-slate-900 border border-amber-500/15 rounded-xl w-full max-w-lg overflow-hidden text-xs">
            <div className="p-4 bg-slate-950/60 border-b border-white/5 flex justify-between items-center">
              <h3 className="font-bold text-slate-100 text-sm">Add Lead</h3>
              <button
                onClick={() => setIsOpenAddModal(false)}
                className="p-1 text-slate-400 hover:text-white hover:bg-white/5 rounded"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={submitAddLead} className="p-5 space-y-4">
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="text-[10px] font-mono text-slate-400 block mb-1">Prospect Name *</label>
                  <input
                    type="text"
                    required
                    placeholder="Siddharth"
                    value={formData.name}
                    onChange={e => setFormData({ ...formData, name: e.target.value })}
                    className="w-full bg-slate-955 border border-slate-800 rounded px-2.5 py-1.5 text-white"
                  />
                </div>
                <div>
                  <label className="text-[10px] font-mono text-slate-400 block mb-1">Mobile number *</label>
                  <input
                    type="text"
                    required
                    placeholder="+91 91234 56789"
                    value={formData.mobile}
                    onChange={e => setFormData({ ...formData, mobile: e.target.value })}
                    className="w-full bg-slate-955 border border-slate-800 rounded px-2.5 py-1.5 text-white"
                  />
                </div>
                <div>
                  <label className="text-[10px] font-mono text-slate-400 block mb-1">Email</label>
                  <input
                    type="email"
                    placeholder="sid@gmail.com"
                    value={formData.email}
                    onChange={e => setFormData({ ...formData, email: e.target.value })}
                    className="w-full bg-slate-955 border border-slate-800 rounded px-2.5 py-1.5 text-white"
                  />
                </div>
                <div>
                  <label className="text-[10px] font-mono text-slate-400 block mb-1">Country</label>
                  <input
                    type="text"
                    value={formData.country}
                    onChange={e => setFormData({ ...formData, country: e.target.value })}
                    className="w-full bg-slate-955 border border-slate-800 rounded px-2.5 py-1.5 text-white"
                  />
                </div>
                <div>
                  <label className="text-[10px] font-mono text-slate-400 block mb-1">Primary Language</label>
                  <select
                    value={formData.language}
                    onChange={e => setFormData({ ...formData, language: e.target.value })}
                    className="w-full bg-slate-955 border border-slate-800 rounded px-2 py-1.5 text-white cursor-pointer"
                  >
                    {languages.map(l => (
                      <option key={l} value={l} className="bg-slate-900">{l}</option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="text-[10px] font-mono text-slate-400 block mb-1">Source Origin</label>
                  <select
                    value={formData.source}
                    onChange={e => setFormData({ ...formData, source: e.target.value as any })}
                    className="w-full bg-slate-955 border border-slate-800 rounded px-2 py-1.5 text-white cursor-pointer"
                  >
                    {sources.map(s => (
                      <option key={s} value={s} className="bg-slate-900">{s}</option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="text-[10px] font-mono text-slate-400 block mb-1">Interest Category</label>
                  <select
                    value={formData.interestType}
                    onChange={e => setFormData({ ...formData, interestType: e.target.value as any })}
                    className="w-full bg-slate-955 border border-slate-800 rounded px-2 py-1.5 text-white cursor-pointer"
                  >
                    <option value="Consultation" className="bg-slate-900">Consultation Enquiry</option>
                    <option value="Course" className="bg-slate-900">Course Academy</option>
                    <option value="Workshop" className="bg-slate-900">Workshop Event</option>
                  </select>
                </div>
                <div>
                  <label className="text-[10px] font-mono text-slate-400 block mb-1">Assign Staff</label>
                  {coworkers && coworkers.length > 0 ? (
                    <select
                      value={formData.assignedStaff}
                      onChange={e => setFormData({ ...formData, assignedStaff: e.target.value })}
                      className="w-full bg-slate-955 border border-slate-800 rounded px-2.5 py-2 text-white cursor-pointer"
                    >
                      <option value="Unassigned">Unassigned</option>
                      {coworkers.map(c => (
                        <option key={c.id} value={c.name} className="bg-slate-900">{c.name} ({c.role})</option>
                      ))}
                    </select>
                  ) : (
                    <input
                      type="text"
                      value={formData.assignedStaff}
                      onChange={e => setFormData({ ...formData, assignedStaff: e.target.value })}
                      className="w-full bg-slate-955 border border-slate-800 rounded px-2.5 py-1.5 text-white"
                    />
                  )}
                </div>
              </div>

              <div>
                <label className="text-[10px] font-mono text-slate-400 block mb-1">Initial Requirements Overview</label>
                <textarea
                  rows={2}
                  placeholder="Input enquirers notes or astrological parameters concerns..."
                  value={formData.notes}
                  onChange={e => setFormData({ ...formData, notes: e.target.value })}
                  className="w-full bg-slate-955 border border-slate-800 rounded p-2 text-white"
                />
              </div>

              <div className="pt-2 flex justify-end gap-2.5">
                <button
                  type="button"
                  onClick={() => setIsOpenAddModal(false)}
                  className="bg-slate-800 hover:bg-slate-750 text-slate-350 border border-slate-705 px-4 py-2 rounded-lg font-bold transition-all cursor-pointer"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="bg-amber-500 hover:bg-amber-450 text-slate-950 font-bold px-4 py-2 rounded-lg shadow-lg shadow-amber-500/10 transition-all cursor-pointer"
                >
                  Capture Lead
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* EXCEL / CSV BULK IMPORT MODAL */}
      {isOpenImportModal && (
        <div className="fixed inset-0 bg-slate-950/60 backdrop-blur-xs z-50 flex justify-center items-center p-4">
          <div className="bg-white rounded-xl shadow-2xl border border-slate-200 max-w-4xl w-full flex flex-col max-h-[90vh] overflow-hidden animate-in fade-in-50 zoom-in-95 duration-200">
            {/* Modal Header */}
            <div className="p-4 border-b border-slate-200 flex justify-between items-center bg-slate-50 shrink-0">
              <div className="flex items-center gap-2">
                <Database className="w-5 h-5 text-amber-600" />
                <div>
                  <h3 className="font-bold text-slate-900 text-sm">Bulk Lead Import System</h3>
                  <p className="text-[10px] text-slate-500">Populate your CRM pipeline instantly from Excel or CSV files</p>
                </div>
              </div>
              <button
                onClick={() => {
                  setIsOpenImportModal(false);
                  setParsedPreviewLeads([]);
                  setImportStatus("idle");
                }}
                className="p-1.5 hover:bg-slate-200 rounded-lg text-slate-500 transition-colors cursor-pointer"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            {/* Modal Content */}
            <div className="p-5 flex-1 overflow-y-auto space-y-4">
              {/* Guidance & Downloader Section */}
              <div className="bg-amber-50/70 border border-amber-200/50 rounded-lg p-3.5 flex flex-col md:flex-row justify-between items-start md:items-center gap-3">
                <div className="space-y-1">
                  <h4 className="text-xs font-bold text-amber-900 flex items-center gap-1.5">
                    <Sparkles className="w-3.5 h-3.5 text-amber-600" />
                    How to prepare your spreadsheet:
                  </h4>
                  <p className="text-[10px] text-amber-800 leading-relaxed max-w-xl">
                    1. For best compatibility, download the template sheet.<br />
                    2. Save your file as a <strong>CSV (Comma Delimited)</strong> or standard Excel sheet in Excel.<br />
                    3. Make sure <strong>Name</strong> and <strong>Mobile</strong> column headers are filled out & present.
                  </p>
                </div>
                <button
                  type="button"
                  onClick={downloadCSVSampleTemplate}
                  className="bg-amber-500 hover:bg-amber-600 text-slate-950 font-bold px-3.5 py-1.5 text-[11px] rounded flex items-center gap-1.5 shadow-xs shrink-0 self-stretch md:self-auto justify-center cursor-pointer"
                >
                  <Download className="w-3.5 h-3.5" />
                  <span>Download CSV Template</span>
                </button>
              </div>

              {/* Upload Zone */}
              {parsedPreviewLeads.length === 0 && (
                <div
                  onDragEnter={handleDrag}
                  onDragOver={handleDrag}
                  onDragLeave={handleDrag}
                  onDrop={handleDrop}
                  className={`border-2 border-dashed rounded-xl p-8 text-center flex flex-col items-center justify-center space-y-2 transition-all ${
                    dragActive ? "border-amber-500 bg-amber-50/30" : "border-slate-200 bg-slate-50/50 hover:bg-slate-50"
                  }`}
                >
                  <div className="w-12 h-12 rounded-full bg-slate-100 flex items-center justify-center">
                    <Upload className="w-6 h-6 text-amber-600" />
                  </div>
                  <div className="space-y-1">
                    <p className="text-xs font-bold text-slate-800">Drag & drop your Excel/CSV here</p>
                    <p className="text-[10px] text-slate-500">or click to browse your local computer files</p>
                  </div>
                  <label className="cursor-pointer bg-white hover:bg-slate-50 border border-slate-300 shadow-xs font-semibold px-3 py-1 text-[11px] rounded text-slate-700">
                    Choose File
                    <input
                      type="file"
                      accept=".csv"
                      onChange={handleFileChange}
                      className="hidden"
                    />
                  </label>
                  <p className="text-[9px] text-slate-400">Supported formats: standard .csv spreadsheet outputs</p>
                </div>
              )}

              {/* Error messages */}
              {importStatus === "error" && (
                <div className="bg-rose-50 border border-rose-200 text-rose-800 p-3 rounded-lg flex items-center gap-2.5 text-xs">
                  <AlertCircle className="w-4 h-4 shrink-0 text-rose-600" />
                  <span>{importErrorMessage}</span>
                </div>
              )}

              {/* Preview parsed Leads table */}
              {parsedPreviewLeads.length > 0 && (
                <div className="space-y-2">
                  <div className="flex justify-between items-center border-b border-slate-200 pb-1.5">
                    <span className="text-xs font-bold text-slate-800">
                      Import Preview: {parsedPreviewLeads.length} Record(s) Found
                    </span>
                    <button
                      type="button"
                      onClick={() => setParsedPreviewLeads([])}
                      className="text-[10px] text-rose-600 hover:underline cursor-pointer"
                    >
                      Clear & Upload Different File
                    </button>
                  </div>

                  <div className="border border-slate-200 rounded-lg overflow-x-auto max-h-[300px]">
                    <table className="w-full text-left text-[10.5px]">
                      <thead className="bg-slate-50 text-slate-600 border-b border-slate-200 font-mono tracking-wider sticky top-0">
                        <tr>
                          <th className="px-3 py-2">Status</th>
                          <th className="px-3 py-2">Name</th>
                          <th className="px-3 py-2">Mobile</th>
                          <th className="px-3 py-2">Language</th>
                          <th className="px-3 py-2">Source</th>
                          <th className="px-3 py-2">Interest</th>
                          <th className="px-3 py-2">Notes</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-slate-100 bg-white">
                        {parsedPreviewLeads.map((item, index) => (
                          <tr key={index} className={item.isValid ? "hover:bg-slate-50/50" : "bg-rose-50/40 hover:bg-rose-50/60"}>
                            <td className="px-3 py-2 whitespace-nowrap">
                              {item.isValid ? (
                                <span className="inline-flex items-center gap-1 text-[9.5px] text-emerald-700 font-bold bg-emerald-50 px-1.5 py-0.5 rounded-sm">
                                  <Check className="w-3 h-3 text-emerald-600" /> Ready
                                </span>
                              ) : (
                                <span className="inline-flex items-center gap-1 text-[9.5px] text-rose-700 font-bold bg-rose-50 px-1.5 py-0.5 rounded-sm" title={item.validationError}>
                                  <AlertCircle className="w-3 h-3 text-rose-500" /> Invalid
                                </span>
                              )}
                            </td>
                            <td className="px-3 py-2 font-semibold text-slate-900 whitespace-nowrap">
                              {item.name || <span className="text-rose-500 italic font-normal">empty name</span>}
                            </td>
                            <td className="px-3 py-2 font-mono whitespace-nowrap">
                              {item.mobile || <span className="text-rose-500 italic font-normal">empty mobile</span>}
                            </td>
                            <td className="px-3 py-2 whitespace-nowrap">{item.language}</td>
                            <td className="px-3 py-2 whitespace-nowrap">
                              <span className="bg-slate-100 text-slate-700 px-1.5 py-0.5 rounded font-mono text-[9px] uppercase">
                                {item.source}
                              </span>
                            </td>
                            <td className="px-3 py-2 whitespace-nowrap">
                              <span className="bg-amber-500/10 text-amber-800 px-1.5 py-0.5 rounded font-semibold">
                                {item.interestType}
                              </span>
                            </td>
                            <td className="px-3 py-2 max-w-[200px] truncate text-slate-500" title={item.notes}>
                              {item.notes}
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>

                  {/* Summary calculations */}
                  <div className="flex gap-4 text-[10px] text-slate-500 font-mono bg-slate-50 border border-slate-200 p-2 rounded-lg">
                    <span>Total Rows: {parsedPreviewLeads.length}</span>
                    <span className="text-emerald-600 font-bold">Valid: {parsedPreviewLeads.filter(l => l.isValid).length}</span>
                    <span className="text-rose-600 font-bold">Failed: {parsedPreviewLeads.filter(l => !l.isValid).length}</span>
                  </div>
                </div>
              )}
            </div>

            {/* Modal Footer */}
            <div className="p-4 border-t border-slate-200 bg-slate-50/80 flex justify-end gap-2.5 shrink-0">
              <button
                type="button"
                onClick={() => {
                  setIsOpenImportModal(false);
                  setParsedPreviewLeads([]);
                  setImportStatus("idle");
                }}
                className="border border-slate-300 hover:bg-slate-100 text-slate-700 px-4 py-2 rounded-lg text-xs font-bold cursor-pointer transition-colors"
              >
                Close
              </button>
              
              {parsedPreviewLeads.length > 0 && (
                <button
                  type="button"
                  disabled={importStatus === "uploading" || parsedPreviewLeads.filter(l => l.isValid).length === 0}
                  onClick={executeBulkImport}
                  className="bg-amber-500 hover:bg-amber-600 disabled:opacity-50 disabled:cursor-not-allowed text-slate-950 px-5 py-2 rounded-lg text-xs font-bold flex items-center gap-1.5 shadow-sm cursor-pointer transition-all"
                >
                  {importStatus === "uploading" ? (
                    <>
                      <div className="w-3.5 h-3.5 border-2 border-slate-950 border-t-transparent rounded-full animate-spin"></div>
                      <span>Importing to CRM...</span>
                    </>
                  ) : importStatus === "success" ? (
                    <>
                      <Check className="w-4 h-4 text-emerald-800" />
                      <span>Import Success!</span>
                    </>
                  ) : (
                    <>
                      <CheckCircle className="w-4 h-4" />
                      <span>Confirm Import ({parsedPreviewLeads.filter(l => l.isValid).length} Leads)</span>
                    </>
                  )}
                </button>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
