import React from "react";
import {
  Sparkles,
  Calendar,
  Layers,
  MapPin,
  Clock,
  Video,
  FileText,
  User,
  Plus,
  Compass,
  AlertCircle,
  TrendingUp,
  RotateCcw,
  Activity
} from "lucide-react";
import { Client, Consultation, ConsultationNote, ConsultationType } from "../types";

interface ConsultationManagementProps {
  clients: Client[];
  consultations: Consultation[];
  onAddClient: (clientData: Partial<Client>) => void;
  onAddConsultation: (consData: Partial<Consultation>) => void;
  onUpdateConsultation: (consId: string, updatedFields: Partial<Consultation>) => void;
}

export function ConsultationManagement({
  clients,
  consultations,
  onAddClient,
  onAddConsultation,
  onUpdateConsultation
}: ConsultationManagementProps) {
  // Input fields state
  const [selectedClient, setSelectedClient] = React.useState<Client | null>(clients[0] || null);
  const [activeConsultation, setActiveConsultation] = React.useState<Consultation | null>(consultations[0] || null);

  // New Client Registration Form
  const [isOpenAddClient, setIsOpenAddClient] = React.useState(false);
  const [newClientData, setNewClientData] = React.useState({
    name: "",
    phone: "",
    email: "",
    dob: "1995-05-15",
    birthTime: "08:30",
    birthPlace: "Chennai, Tamil Nadu",
    address: "",
    occupation: "Software Architect",
    country: "India",
    language: "Tamil"
  });

  // Scheduling block
  const [isOpenSchedule, setIsOpenSchedule] = React.useState(false);
  const [scheduleData, setScheduleData] = React.useState({
    clientId: "",
    type: "Career" as ConsultationType,
    dateTime: "2026-06-25T11:00",
    astrologerId: "astro-muralidharan",
    astrologerName: "Dr. K. Muralidharan",
    meetingLink: "https://meet.google.com/xyz-uvwx-123"
  });

  // Astronomical Analysis state (Gemini integration output)
  const [isAILoading, setIsAILoading] = React.useState(false);
  const [aiReport, setAiReport] = React.useState<string | null>(null);
  const [parsedLagna, setParsedLagna] = React.useState<string | null>(null);
  const [parsedProgressLagna, setParsedProgressLagna] = React.useState<string | null>(null);
  const [calculatedAge, setCalculatedAge] = React.useState<number | null>(null);

  // Session Note entry
  const [sessionNotesInput, setSessionNotesInput] = React.useState("");
  const [sessionNotesList, setSessionNotesList] = React.useState<ConsultationNote[]>([]);

  const consultationTypes: ConsultationType[] = [
    "Career",
    "Marriage",
    "Business",
    "Health",
    "Education",
    "Foreign Settlement",
    "General Guidance",
    "Financial Prosperity"
  ];

  // Fetch consultation notes when active consultation changes
  React.useEffect(() => {
    if (activeConsultation) {
      fetch(`/api/consultations/${activeConsultation.id}/notes`)
        .then(res => res.json())
        .then(data => setSessionNotesList(data || []))
        .catch(err => console.error("Error loading consultation notes", err));
    }
  }, [activeConsultation]);

  const handleRegisterClient = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newClientData.name || !newClientData.phone) {
      alert("Client Name and Mobile contact are required coordinate lines.");
      return;
    }
    onAddClient(newClientData);
    setIsOpenAddClient(false);
    // Reset
    setNewClientData({
      name: "",
      phone: "",
      email: "",
      dob: "1995-05-15",
      birthTime: "08:30",
      birthPlace: "Chennai, Tamil Nadu",
      address: "",
      occupation: "Software Architect",
      country: "India",
      language: "Tamil"
    });
  };

  const handleScheduleSession = (e: React.FormEvent) => {
    e.preventDefault();
    if (!scheduleData.clientId || !scheduleData.dateTime) {
      alert("Please select client context and date/time coordinates.");
      return;
    }

    const cName = clients.find(c => c.id === scheduleData.clientId)?.name || "Client";
    onAddConsultation({
      ...scheduleData,
      clientName: cName,
      dateTime: new Date(scheduleData.dateTime).toISOString()
    });

    setIsOpenSchedule(false);
  };

  // Submit session note
  const handleSubmitSessionNote = (e: React.FormEvent) => {
    e.preventDefault();
    if (!activeConsultation || !sessionNotesInput.trim()) return;

    fetch(`/api/consultations/${activeConsultation.id}/notes`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        title: "Astrologer Session Note",
        content: sessionNotesInput
      })
    })
      .then(res => res.json())
      .then(newNote => {
        setSessionNotesList(prev => [...prev, newNote]);
        setSessionNotesInput("");
        alert("Session log added to client consultation database histories!");
      })
      .catch(err => console.error("Error logging session note", err));
  };

  // GENLINE GEMINI API ALP COMPUTE TRIGGER
  const triggerAIEngineAnalysis = async () => {
    if (!selectedClient) {
      alert("Please select or register client profile context first");
      return;
    }

    setIsAILoading(true);
    setAiReport(null);

    const fallbackNotes = activeConsultation 
      ? `Active scheduling: ${activeConsultation.type} consultation on date ${activeConsultation.dateTime}`
      : "Ad-hoc astrological timing analysis.";

    try {
      const response = await fetch("/api/ai/analyze-chart", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          clientName: selectedClient.name,
          dob: selectedClient.dob,
          birthTime: selectedClient.birthTime,
          birthPlace: selectedClient.birthPlace,
          consultationType: activeConsultation?.type || "General Guidance",
          notes: fallbackNotes
        })
      });

      const resData = await response.json();
      if (resData.success) {
        setAiReport(resData.reportText);
        setParsedLagna(resData.natalLagna);
        setParsedProgressLagna(resData.progressedLagna);
        setCalculatedAge(resData.age);
      } else {
        alert("Astro AI computation experienced errors, please retry.");
      }
    } catch (error) {
      console.error("AI Astrology Gateway failed", error);
      alert("Astro AI Engine failed to respond. Launching localized mathematics backup...");
    } finally {
      setIsAILoading(false);
    }
  };

  // ALP Circle Rasi Coordinates helper rendering
  const zodiacSigns = [
    "Aries", "Taurus", "Gemini", "Cancer",
    "Leo", "Virgo", "Libra", "Scorpio",
    "Sagittarius", "Capricorn", "Aquarius", "Pisces"
  ];

  return (
    <div className="space-y-6">
      {/* Client Context Selection & Registration Strip */}
      <div className="flex flex-col lg:flex-row justify-between items-start lg:items-center gap-4 bg-slate-900 border border-slate-800 rounded-xl p-4">
        <div className="flex flex-wrap items-center gap-4 w-full lg:w-auto text-xs">
          <div>
            <label className="text-[10px] font-mono uppercase text-slate-500 block mb-1">Active Client Profile</label>
            <select
              value={selectedClient?.id || ""}
              onChange={e => {
                const targetId = e.target.value;
                const found = clients.find(c => c.id === targetId);
                if (found) {
                  setSelectedClient(found);
                  // Auto lookup matching active consultation for quick logs loading
                  const matchingCons = consultations.find(c => c.clientId === found.id);
                  if (matchingCons) setActiveConsultation(matchingCons);
                }
              }}
              className="bg-slate-950 border border-slate-800 text-slate-200 px-3 py-2 rounded-lg text-xs cursor-pointer focus:outline-none"
            >
              <option value="" disabled>Select client register...</option>
              {clients.map(c => (
                <option key={c.id} value={c.id} className="bg-slate-900">{c.name} ({c.dob})</option>
              ))}
            </select>
          </div>

          {selectedClient && (
            <div className="flex items-center gap-5 border-l border-white/10 pl-5 text-[11px] text-slate-350">
              <div className="space-y-0.5">
                <span className="text-slate-500 font-mono block text-[9px] uppercase">Birth Coordinates</span>
                <p className="text-slate-200 font-medium">{selectedClient.dob} | {selectedClient.birthTime}</p>
              </div>
              <div className="space-y-0.5">
                <span className="text-slate-500 font-mono block text-[9px] uppercase">Birth Place</span>
                <p className="text-slate-200 font-medium truncate max-w-44">{selectedClient.birthPlace}</p>
              </div>
            </div>
          )}
        </div>

        <div className="flex gap-2 w-full lg:w-auto justify-end">
          <button
            onClick={() => setIsOpenAddClient(true)}
            className="border border-amber-500/20 text-amber-400 hover:bg-amber-500 hover:text-slate-950 px-3 py-2 rounded-lg text-xs font-bold transition-all cursor-pointer flex items-center justify-center gap-1.5"
          >
            <Plus className="w-4 h-4" />
            <span>Onboard Client Profile</span>
          </button>
          <button
            onClick={() => {
              if (selectedClient) {
                setScheduleData(prev => ({ ...prev, clientId: selectedClient.id }));
              }
              setIsOpenSchedule(true);
            }}
            className="bg-amber-500 hover:bg-amber-450 text-slate-950 px-4 py-2 rounded-lg text-xs font-bold transition-all cursor-pointer flex items-center justify-center gap-1.5"
          >
            <Calendar className="w-4 h-4" />
            <span>Book Consultation Slot</span>
          </button>
        </div>
      </div>

      {/* Split Consultations workspace layout */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        
        {/* Left Column (8 units) - Astro progression engine & AI report */}
        <div className="lg:col-span-8 space-y-6">
          {/* ALP Wheel & Compute Dashboard */}
          <div className="bg-slate-900/80 border border-white/5 rounded-xl p-5 shadow-sm">
            <div className="flex justify-between items-center mb-5 border-b border-white/5 pb-3">
              <div className="flex items-center gap-2">
                <Compass className="w-5 h-5 text-amber-400 shrink-0" />
                <h3 className="font-bold text-sm text-slate-100">ALP Chart Progressive Mathematics</h3>
              </div>
              {selectedClient && (
                <button
                  onClick={triggerAIEngineAnalysis}
                  disabled={isAILoading}
                  className="bg-linear-to-r from-amber-500 to-yellow-400 text-slate-950 font-bold px-4 py-2 rounded-lg text-[11px] hover:from-amber-450 flex items-center gap-1.5 shadow-lg shadow-amber-500/5 cursor-pointer disabled:opacity-50"
                >
                  <Sparkles className="w-3.5 h-3.5 animate-pulse" />
                  <span>{isAILoading ? "ALP Calculations Active..." : "Run ALP Gemini Engine"}</span>
                </button>
              )}
            </div>

            {selectedClient ? (
              <div className="grid grid-cols-1 md:grid-cols-12 gap-6 items-center">
                {/* CIRCULAR ASTRO SVG CHART CANVAS REPRESENTATION (5 Units) */}
                <div className="md:col-span-5 flex flex-col items-center justify-center bg-slate-950/40 p-4 border border-white/5 rounded-lg">
                  <div className="relative w-48 h-48">
                    {/* SVG Circular Dial */}
                    <svg className="w-full h-full transform -rotate-90 select-none" viewBox="0 0 100 100">
                      <circle cx="50" cy="50" r="45" fill="none" stroke="#2a3c5a" strokeWidth="1" />
                      {/* Outer zodiac rays segments (12 Signs of 30 deg each) */}
                      {Array.from({ length: 12 }).map((_, rIdx) => {
                        const deg = rIdx * 30;
                        const rad = (deg * Math.PI) / 180;
                        const x1 = 50 + 40 * Math.cos(rad);
                        const y1 = 50 + 40 * Math.sin(rad);
                        const x2 = 50 + 45 * Math.cos(rad);
                        const y2 = 50 + 45 * Math.sin(rad);

                        // Label positions
                        const radLabel = ((deg + 15) * Math.PI) / 180;
                        const tx = 50 + 35 * Math.cos(radLabel);
                        const ty = 50 + 35 * Math.sin(radLabel);

                        // Checks if this segment match natal or progressed lagna
                        const isNatal = parsedLagna?.toLowerCase().includes(zodiacSigns[rIdx].toLowerCase());
                        const isProgress = parsedProgressLagna?.toLowerCase().includes(zodiacSigns[rIdx].toLowerCase());

                        let strokeColor = "#1e293b";
                        let strokeWidthVal = "0.5";
                        if (isNatal) strokeColor = "#3b82f6";
                        if (isProgress) strokeColor = "#f59e0b";

                        return (
                          <g key={rIdx}>
                            <line x1={x1} y1={y1} x2={x2} y2={y2} stroke="#3b82f6" strokeWidth="0.5" strokeOpacity="0.4" />
                            {/* Inner slice highlights for natal vs progressed */}
                            <path
                              d={`M 50 50 L ${x2} ${y2} A 45 45 0 0 1 ${50 + 45 * Math.cos(rad + (30 * Math.PI) / 180)} ${50 + 45 * Math.sin(rad + (30 * Math.PI) / 180)} Z`}
                              fill={isProgress ? "rgba(245,158,11,0.06)" : isNatal ? "rgba(59,130,246,0.06)" : "none"}
                              stroke={isProgress ? "#f59e0b" : isNatal ? "#3b82f6" : "rgba(255,255,255,0.05)"}
                              strokeWidth={isProgress || isNatal ? "1.5" : "0.3"}
                            />
                            <text
                              x={tx}
                              y={ty}
                              fill={isProgress ? "#f59e0b" : isNatal ? "#3b82f6" : "#64748b"}
                              fontSize="4"
                              textAnchor="middle"
                              alignmentBaseline="middle"
                              className="font-semibold font-sans uppercase"
                              transform={`rotate(90 ${tx} ${ty})`}
                            >
                              {zodiacSigns[rIdx].substring(0, 3)}
                            </text>
                          </g>
                        );
                      })}
                      <circle cx="50" cy="50" r="10" fill="#0f172a" stroke="#f59e0b" strokeWidth="0.5" strokeOpacity="0.3" />
                      <text x="50" y="52" fill="#f59e0b" fontSize="6.5" fontWeight="bold" textAnchor="middle" className="font-mono">
                        {calculatedAge || "30"}
                      </text>
                    </svg>

                    {/* Left corner mini absolute coordinates flags */}
                    <div className="absolute top-0 right-0 p-1.5 flex flex-col gap-1 text-[8px] font-mono">
                      <div className="flex items-center gap-1">
                        <span className="w-1.5 h-1.5 rounded-full bg-blue-500"></span>
                        <span className="text-blue-400">Natal Asc</span>
                      </div>
                      <div className="flex items-center gap-1">
                        <span className="w-1.5 h-1.5 rounded-full bg-amber-500 animate-pulse"></span>
                        <span className="text-amber-500 font-bold">ALP Prog</span>
                      </div>
                    </div>
                  </div>

                  <div className="mt-3.5 space-y-1 text-center font-mono text-[10px] text-slate-400">
                    <p>Mathematics: <span className="text-amber-400">Progressed 1° per year</span></p>
                    <p>Current Lagna: <span className="text-slate-100 font-bold">{parsedProgressLagna || "Calculated on click"}</span></p>
                  </div>
                </div>

                {/* COMPUTATION RESULTS PROFILE DISPLAY (7 Units) */}
                <div className="md:col-span-7 space-y-4">
                  <div className="bg-slate-950/30 border border-white/5 rounded-lg p-4 space-y-3">
                    <h4 className="font-semibold text-slate-200 uppercase tracking-wider text-[10px] font-mono">
                      Planetary Shift Analysis Coordinates
                    </h4>
                    <div className="grid grid-cols-2 gap-3 text-[10px] font-mono text-slate-400">
                      <div className="bg-slate-950 p-2 rounded">
                        <p className="text-slate-500">NATAL ASCENDENT</p>
                        <p className="text-slate-200 font-bold text-xs mt-0.5">{parsedLagna || "N/A"}</p>
                      </div>
                      <div className="bg-slate-950 p-2 rounded">
                        <p className="text-slate-500 font-bold text-amber-500">ALP PROGRESSION SIGN</p>
                        <p className="text-amber-400 font-bold text-xs mt-0.5">{parsedProgressLagna || "N/A"}</p>
                      </div>
                      <div className="bg-slate-950 p-2 rounded">
                        <p className="text-slate-500">COMPUTED CURRENT AGE</p>
                        <p className="text-slate-200 font-bold text-xs mt-0.5">{calculatedAge ? `${calculatedAge} Years` : "N/A"}</p>
                      </div>
                      <div className="bg-slate-950 p-2 rounded">
                        <p className="text-slate-500">DEGREES OFFSET</p>
                        <p className="text-slate-200 font-bold text-xs mt-0.5">{(calculatedAge || 30) % 30}° orbital shift</p>
                      </div>
                    </div>

                    <div className="bg-slate-950 p-3 rounded text-[10px] text-slate-400 leading-relaxed font-mono flex gap-2">
                      <TrendingUp className="w-4 h-4 text-emerald-400 shrink-0 mt-0.5" />
                      <span>
                        ALP Mathematical progression correlates biological age directly onto cosmological coordinates. In Scorpio birth alignments, an age of 30 transitions active energies directly into Taurus house structures.
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            ) : (
              <div className="text-center py-16 text-slate-500 text-xs">
                <AlertCircle className="w-8 h-8 text-amber-400/20 mx-auto mb-2" />
                Select client context to render Akshaya Lagna math circles.
              </div>
            )}
          </div>

          {/* Detailed Astrology Reading / Gemini Output report */}
          <div className="bg-slate-900 border border-white/5 rounded-xl p-5 shadow-sm">
            <h3 className="font-bold text-sm text-slate-200 mb-4 flex items-center gap-1.5">
              <Sparkles className="w-4 h-4 text-amber-400" />
              <span>ALP Gemini Advanced Assessment Report</span>
            </h3>

            {isAILoading && (
              <div className="text-center py-20 text-slate-400 font-mono space-y-3">
                <Activity className="w-8 h-8 text-amber-400 animate-spin mx-auto" />
                <p className="text-xs">Invoking Deep Gemini-2.5 Astrological Synthesis Core APIs...</p>
                <p className="text-[10px] text-slate-600">Resolving cosmic vectors & coordinate intersections</p>
              </div>
            )}

            {!isAILoading && aiReport && (
              <div className="bg-slate-950/50 rounded-lg p-5 leading-relaxed text-xs text-slate-300 font-sans border border-white/5 whitespace-pre-wrap max-h-[420px] overflow-y-auto">
                {aiReport}
              </div>
            )}

            {!isAILoading && !aiReport && (
              <div className="text-center py-24 text-slate-500 text-xs border border-dashed border-white/5 rounded-lg flex flex-col justify-center items-center">
                <Compass className="w-8 h-8 text-amber-500/20 mb-2" />
                <span>Astrology engine reports will populate here once calculations has been processed.</span>
              </div>
            )}
          </div>
        </div>

        {/* Right Column (4 units) - Appointments calendar context and session files notes */}
        <div className="lg:col-span-4 space-y-6">
          {/* Active Consultation context Selector */}
          <div className="bg-slate-900 border border-white/5 rounded-xl p-5 shadow-sm text-xs space-y-4">
            <h3 className="font-bold text-sm text-slate-200 font-sans">Active Appointment Slot</h3>
            
            <div className="space-y-2 max-h-56 overflow-y-auto pr-1">
              {consultations
                .filter(c => !selectedClient || c.clientId === selectedClient.id)
                .map(cons => (
                  <div
                    key={cons.id}
                    onClick={() => setActiveConsultation(cons)}
                    className={`p-3 rounded-lg border text-left cursor-pointer transition-colors ${
                      activeConsultation?.id === cons.id
                        ? "bg-amber-500/10 border-amber-500/30"
                        : "bg-slate-950/40 border-white/5 hover:border-slate-800"
                    }`}
                  >
                    <div className="flex justify-between items-center mb-1">
                      <span className="font-bold text-slate-200">{cons.clientName}</span>
                      <span className="text-[9px] bg-slate-800 text-slate-300 px-1.5 py-0.5 rounded uppercase font-mono">
                        {cons.type}
                      </span>
                    </div>
                    <div className="text-[10.5px] text-slate-400 font-mono space-y-0.5 mt-2">
                      <p>Advisor: {cons.astrologerName}</p>
                      <p>Time: {new Date(cons.dateTime).toLocaleString([], { dateStyle: "short", timeStyle: "short" })}</p>
                    </div>
                  </div>
                ))}

              {consultations.filter(c => !selectedClient || c.clientId === selectedClient.id).length === 0 && (
                <p className="text-center py-6 text-slate-600">No scheduled consultations found.</p>
              )}
            </div>

            {activeConsultation && (
              <div className="bg-slate-950/60 p-4 rounded-lg border border-white/5 space-y-3 pt-4">
                <div className="flex justify-between items-center border-b border-white/5 pb-2">
                  <span className="font-bold text-amber-500 uppercase tracking-widest text-[9px] font-mono">Session Info Card</span>
                  <span className="text-[9px] text-emerald-400 bg-emerald-500/5 px-2 py-0.5 rounded-full font-mono">{activeConsultation.status}</span>
                </div>
                
                <div className="space-y-1 text-[11px] text-slate-350">
                  <p className="flex items-center gap-2"><User className="w-3.5 h-3.5 text-slate-500 shrink-0" /> <span className="text-slate-150 font-semibold">{activeConsultation.clientName}</span></p>
                  <p className="flex items-center gap-2"><Clock className="w-3.5 h-3.5 text-slate-500 shrink-0" /> <span>{new Date(activeConsultation.dateTime).toLocaleString()}</span></p>
                </div>

                <div className="flex gap-2 pt-2">
                  <a
                    href={activeConsultation.meetingLink}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="flex-1 bg-slate-800 hover:bg-slate-750 text-slate-300 rounded py-2 text-center hover:text-white transition-colors flex items-center justify-center gap-1 px-1.5 text-[10.5px] font-semibold"
                  >
                    <Video className="w-3.5 h-3.5" />
                    <span>Launch Teleconference</span>
                  </a>
                  {activeConsultation.status === "Scheduled" && (
                    <button
                      onClick={() => onUpdateConsultation(activeConsultation.id, { status: "Completed" })}
                      className="bg-emerald-500 hover:bg-emerald-450 text-slate-950 font-bold px-3 py-2 rounded transition-all cursor-pointer text-[10.5px]"
                    >
                      Complete
                    </button>
                  )}
                </div>
              </div>
            )}
          </div>

          {/* Active Consultation Session Notes Log Form */}
          {activeConsultation && (
            <div className="bg-slate-900 border border-white/5 rounded-xl p-5 shadow-sm text-xs space-y-4 flex flex-col h-[320px]">
              <div className="flex items-center gap-1.5 shrink-0">
                <FileText className="w-4 h-4 text-purple-400" />
                <h3 className="font-bold text-slate-200">Session Notes & Prescriptions</h3>
              </div>
              
              <div className="flex-1 overflow-y-auto space-y-3.5 pr-1 max-h-48">
                {sessionNotesList.map(note => (
                  <div key={note.id} className="bg-slate-950/40 p-2.5 rounded border border-white/5 space-y-1">
                    <div className="flex justify-between items-center text-[9px] text-slate-500">
                      <span>{note.writerName}</span>
                      <span>{new Date(note.timestamp).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}</span>
                    </div>
                    <p className="text-[11px] text-slate-300 whitespace-pre-wrap">{note.content}</p>
                  </div>
                ))}

                {sessionNotesList.length === 0 && (
                  <p className="text-center py-8 text-slate-600 font-mono text-[10px]">No prescription entries recorded.</p>
                )}
              </div>

              <form onSubmit={handleSubmitSessionNote} className="shrink-0 space-y-2 mt-auto">
                <textarea
                  rows={2}
                  placeholder="Record prescription notes or remedy fast cycles advise..."
                  value={sessionNotesInput}
                  onChange={e => setSessionNotesInput(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-800 rounded p-2 text-white focus:outline-none"
                />
                <button
                  type="submit"
                  className="w-full bg-slate-800 hover:bg-slate-755 text-slate-300 font-semibold py-1.5 rounded text-[10px]"
                >
                  Append Note to Ledger
                </button>
              </form>
            </div>
          )}
        </div>
      </div>

      {/* CLIENT MANUAL REGISTRATION MODAL */}
      {isOpenAddClient && (
        <div className="fixed inset-0 bg-slate-950/80 backdrop-blur-xs flex items-center justify-center z-50 p-4">
          <div className="bg-slate-900 border border-amber-500/15 rounded-xl w-full max-w-lg overflow-hidden text-xs">
            <div className="p-4 bg-slate-950/65 border-b border-white/5 flex justify-between items-center">
              <h3 className="font-bold text-slate-100 text-sm">Onboard Astro Client Profile</h3>
              <button onClick={() => setIsOpenAddClient(false)} className="text-slate-400 hover:text-white p-1 rounded">
                ✕
              </button>
            </div>

            <form onSubmit={handleRegisterClient} className="p-5 space-y-4">
              <div className="grid grid-cols-2 gap-3.5">
                <div>
                  <label className="text-[10px] font-mono text-slate-400 block mb-1">Legal Name *</label>
                  <input
                    type="text"
                    required
                    placeholder="Siddharth Raman"
                    value={newClientData.name}
                    onChange={e => setNewClientData({ ...newClientData, name: e.target.value })}
                    className="w-full bg-slate-955 border border-slate-800 rounded px-2.5 py-1.5 text-white"
                  />
                </div>
                <div>
                  <label className="text-[10px] font-mono text-slate-400 block mb-1">Phone Contact *</label>
                  <input
                    type="text"
                    required
                    placeholder="+91 95000 11223"
                    value={newClientData.phone}
                    onChange={e => setNewClientData({ ...newClientData, phone: e.target.value })}
                    className="w-full bg-slate-955 border border-slate-800 rounded px-2.5 py-1.5 text-white"
                  />
                </div>
                <div>
                  <label className="text-[10px] font-mono text-slate-400 block mb-1">Email</label>
                  <input
                    type="email"
                    placeholder="sid@gmail.com"
                    value={newClientData.email}
                    onChange={e => setNewClientData({ ...newClientData, email: e.target.value })}
                    className="w-full bg-slate-955 border border-slate-800 rounded px-2.5 py-1.5 text-white"
                  />
                </div>
                <div>
                  <label className="text-[10px] font-mono text-slate-400 block mb-1">Date of Birth (YYYY-MM-DD) *</label>
                  <input
                    type="date"
                    required
                    value={newClientData.dob}
                    onChange={e => setNewClientData({ ...newClientData, dob: e.target.value })}
                    className="w-full bg-slate-955 border border-slate-800 rounded px-2.5 py-1.5 text-white cursor-pointer"
                  />
                </div>
                <div>
                  <label className="text-[10px] font-mono text-slate-400 block mb-1">Birth Time (HH:MM) *</label>
                  <input
                    type="text"
                    required
                    placeholder="14:35"
                    value={newClientData.birthTime}
                    onChange={e => setNewClientData({ ...newClientData, birthTime: e.target.value })}
                    className="w-full bg-slate-955 border border-slate-800 rounded px-2.5 py-1.5 text-white"
                  />
                </div>
                <div>
                  <label className="text-[10px] font-mono text-slate-400 block mb-1">Birth Place *</label>
                  <input
                    type="text"
                    required
                    placeholder="Madurai, Tamil Nadu"
                    value={newClientData.birthPlace}
                    onChange={e => setNewClientData({ ...newClientData, birthPlace: e.target.value })}
                    className="w-full bg-slate-955 border border-slate-800 rounded px-2.5 py-1.5 text-white"
                  />
                </div>
                <div>
                  <label className="text-[10px] font-mono text-slate-400 block mb-1">Language preferred</label>
                  <input
                    type="text"
                    value={newClientData.language}
                    onChange={e => setNewClientData({ ...newClientData, language: e.target.value })}
                    className="w-full bg-slate-955 border border-slate-800 rounded px-2.5 py-1.5 text-white"
                  />
                </div>
                <div>
                  <label className="text-[10px] font-mono text-slate-400 block mb-1">Occupation</label>
                  <input
                    type="text"
                    value={newClientData.occupation}
                    onChange={e => setNewClientData({ ...newClientData, occupation: e.target.value })}
                    className="w-full bg-slate-955 border border-slate-800 rounded px-2.5 py-1.5 text-white"
                  />
                </div>
              </div>

              <div>
                <label className="text-[10px] font-mono text-slate-400 block mb-1">Home address</label>
                <textarea
                  rows={2}
                  value={newClientData.address}
                  onChange={e => setNewClientData({ ...newClientData, address: e.target.value })}
                  className="w-full bg-slate-955 border border-slate-800 rounded p-2 text-white"
                />
              </div>

              <div className="pt-2 flex justify-end gap-2 text-xs font-bold">
                <button
                  type="button"
                  onClick={() => setIsOpenAddClient(false)}
                  className="bg-slate-800 hover:bg-slate-750 text-slate-350 border border-slate-700 px-4 py-2 rounded-lg cursor-pointer"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="bg-amber-500 hover:bg-amber-450 text-slate-950 px-4 py-2 rounded-lg shadow-lg shadow-amber-500/10 cursor-pointer"
                >
                  Create Client Records
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* SCHEDULE DETAILED SLOT SESSION MODAL */}
      {isOpenSchedule && (
        <div className="fixed inset-0 bg-slate-950/80 backdrop-blur-xs flex items-center justify-center z-50 p-4">
          <div className="bg-slate-900 border border-amber-500/15 rounded-xl w-full max-w-md overflow-hidden text-xs">
            <div className="p-4 bg-slate-950/65 border-b border-light/5 flex justify-between items-center">
              <h3 className="font-bold text-slate-100 text-sm">Schedule Astro Consultation Session</h3>
              <button onClick={() => setIsOpenSchedule(false)} className="text-slate-400 hover:text-white p-1 rounded">
                ✕
              </button>
            </div>

            <form onSubmit={handleScheduleSession} className="p-5 space-y-4">
              <div>
                <label className="text-[10px] font-mono text-slate-400 block mb-1">Match Client Profile Context *</label>
                <select
                  required
                  value={scheduleData.clientId}
                  onChange={e => setScheduleData({ ...scheduleData, clientId: e.target.value })}
                  className="w-full bg-slate-955 border border-slate-800 rounded px-2.5 py-1.5 text-white cursor-pointer"
                >
                  <option value="" disabled>Select client target...</option>
                  {clients.map(c => (
                    <option key={c.id} value={c.id} className="bg-slate-900">{c.name}</option>
                  ))}
                </select>
              </div>

              <div className="grid grid-cols-2 gap-3.5">
                <div>
                  <label className="text-[10px] font-mono text-slate-400 block mb-1">Consultation Category *</label>
                  <select
                    value={scheduleData.type}
                    onChange={e => setScheduleData({ ...scheduleData, type: e.target.value as any })}
                    className="w-full bg-slate-955 border border-slate-800 rounded px-2 py-1.5 text-white cursor-pointer"
                  >
                    {consultationTypes.map(t => (
                      <option key={t} value={t} className="bg-slate-900">{t}</option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="text-[10px] font-mono text-slate-400 block mb-1">Meeting Time (ISO Local) *</label>
                  <input
                    type="datetime-local"
                    required
                    value={scheduleData.dateTime}
                    onChange={e => setScheduleData({ ...scheduleData, dateTime: e.target.value })}
                    className="w-full bg-slate-955 border border-slate-800 rounded px-2.5 py-1.5 text-white cursor-pointer"
                  />
                </div>
                <div>
                  <label className="text-[10px] font-mono text-slate-400 block mb-1">Target Advisor Astrologer</label>
                  <input
                    type="text"
                    value={scheduleData.astrologerName}
                    onChange={e => setScheduleData({ ...scheduleData, astrologerName: e.target.value })}
                    className="w-full bg-slate-955 border border-slate-800 rounded px-2.5 py-1.5 text-white"
                  />
                </div>
                <div>
                  <label className="text-[10px] font-mono text-slate-400 block mb-1">Videoconferencing link</label>
                  <input
                    type="text"
                    value={scheduleData.meetingLink}
                    onChange={e => setScheduleData({ ...scheduleData, meetingLink: e.target.value })}
                    className="w-full bg-slate-955 border border-slate-800 rounded px-2.5 py-1.5 text-white"
                  />
                </div>
              </div>

              <div className="pt-2 flex justify-end gap-2 text-xs font-bold">
                <button
                  type="button"
                  onClick={() => setIsOpenSchedule(false)}
                  className="bg-slate-800 hover:bg-slate-750 text-slate-350 border border-slate-700 px-4 py-2 rounded-lg cursor-pointer"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="bg-amber-500 hover:bg-amber-450 text-slate-950 px-4 py-2 rounded-lg shadow-lg shadow-amber-500/10 cursor-pointer"
                >
                  Confirm Appointment
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
