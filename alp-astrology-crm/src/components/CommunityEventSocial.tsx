import React from "react";
import {
  Users,
  CalendarDays,
  Share2,
  Plus,
  MapPin,
  CheckCircle,
  Tag,
  Clock,
  PlaySquare,
  Facebook,
  Instagram,
  Compass
} from "lucide-react";
import { CommunityMember, Event, SocialMediaPost } from "../types";

interface CommunityEventSocialProps {
  members: CommunityMember[];
  events: Event[];
  socialPosts: SocialMediaPost[];
  onAddMember: (memberData: Partial<CommunityMember>) => void;
  onAddEvent: (eventData: Partial<Event>) => void;
  onBookEventTicket: (eventId: string, ticketData: { memberName: string; memberEmail: string; ticketCount: number }) => void;
  onSchedulePost: (postData: Partial<SocialMediaPost>) => void;
}

export function CommunityEventSocial({
  members,
  events,
  socialPosts,
  onAddMember,
  onAddEvent,
  onBookEventTicket,
  onSchedulePost
}: CommunityEventSocialProps) {
  const [activeSegment, setActiveSegment] = React.useState<"community" | "events" | "social">("community");

  // Filter category community
  const [activeCategoryFilter, setActiveCategoryFilter] = React.useState("All");

  // Add Member Modal toggle
  const [isOpenAddM, setIsOpenAddM] = React.useState(false);
  const [newM, setNewM] = React.useState({
    name: "",
    email: "",
    category: "Premium Members" as any,
    activeGroup: "Global VIP Astrology Circle",
    location: "Chennai, India"
  });

  // Schedule Post Form
  const [isOpenAddPost, setIsOpenAddPost] = React.useState(false);
  const [newPost, setNewPost] = React.useState({
    platform: "YouTube" as any,
    title: "",
    scheduledDate: "2026-06-25",
    campaignName: "Q3 Academy Enrollments"
  });

  // Book Ticket form
  const [activeEventTicket, setActiveEventTicket] = React.useState<Event | null>(null);
  const [ticketForm, setTicketForm] = React.useState({
    memberName: "",
    memberEmail: "",
    ticketCount: 1
  });

  const categories = ["Students", "Consultants", "Researchers", "Volunteers", "Premium Members"];
  const platforms = ["YouTube", "Instagram", "Facebook", "Threads", "ShareChat"];

  const filteredMembers = members.filter(
    m => activeCategoryFilter === "All" || m.category.toLowerCase().includes(activeCategoryFilter.toLowerCase().substring(0, 5))
  );

  const submitMember = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newM.name || !newM.email) return;
    onAddMember(newM);
    setIsOpenAddM(false);
    setNewM({ name: "", email: "", category: "Premium Members", activeGroup: "Global VIP Astrology Circle", location: "Chennai, India" });
  };

  const submitPost = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newPost.title) return;
    onSchedulePost(newPost);
    setIsOpenAddPost(false);
    setNewPost({ platform: "YouTube", title: "", scheduledDate: "2026-06-25", campaignName: "Q3 Academy Enrollments" });
  };

  const submitTicketBooking = (e: React.FormEvent) => {
    e.preventDefault();
    if (!activeEventTicket || !ticketForm.memberName || !ticketForm.memberEmail) return;

    onBookEventTicket(activeEventTicket.id, ticketForm);
    setActiveEventTicket(null);
    setTicketForm({ memberName: "", memberEmail: "", ticketCount: 1 });
    alert("Ticket reservation processed successfully! Check structural payments files ledger.");
  };

  return (
    <div className="space-y-6">
      {/* Sub menu controls */}
      <div className="flex justify-between items-center border-b border-white/5 pb-3 flex-wrap gap-4 text-xs">
        <div className="flex gap-2">
          {[
            { id: "community", label: "Members Directory", icon: Users },
            { id: "events", label: "Workshop Events", icon: CalendarDays },
            { id: "social", label: "Social Media CRM", icon: Share2 }
          ].map(sb => {
            const Icon = sb.icon;
            const isMatch = activeSegment === sb.id;
            return (
              <button
                key={sb.id}
                onClick={() => setActiveSegment(sb.id as any)}
                className={`flex items-center gap-2 px-3 py-1.5 rounded-lg border transition-all cursor-pointer ${
                  isMatch
                    ? "bg-amber-500/10 border-amber-500/20 text-amber-400 font-bold"
                    : "bg-slate-900 border-slate-800 text-slate-400 hover:text-white"
                }`}
              >
                <Icon className="w-4 h-4 shrink-0" />
                <span>{sb.label}</span>
              </button>
            );
          })}
        </div>

        {activeSegment === "community" && (
          <button
            onClick={() => setIsOpenAddM(true)}
            className="bg-slate-800 hover:bg-slate-750 text-slate-200 border border-slate-700 px-4 py-1.5 rounded-lg text-xs font-bold transition-all cursor-pointer"
          >
            Add Member
          </button>
        )}

        {activeSegment === "social" && (
          <button
            onClick={() => setIsOpenAddPost(true)}
            className="bg-amber-500 hover:bg-amber-450 text-slate-950 px-4 py-1.5 rounded-lg text-xs font-bold transition-all cursor-pointer"
          >
            Post Schedule Planner
          </button>
        )}
      </div>

      {/* RENDER MODULARS SEGMENTS */}

      {/* 1. COMMUNITY MEMBERS ROSTER */}
      {activeSegment === "community" && (
        <div className="space-y-4">
          <div className="flex flex-wrap gap-1.5">
            <button
              onClick={() => setActiveCategoryFilter("All")}
              className={`px-3 py-1 rounded text-[10.5px] font-medium ${
                activeCategoryFilter === "All" ? "bg-amber-500 text-slate-950 font-bold" : "bg-slate-900 text-slate-400 hover:text-white"
              }`}
            >
              All Guilds
            </button>
            {categories.map(cat => (
              <button
                key={cat}
                onClick={() => setActiveCategoryFilter(cat)}
                className={`px-3 py-1 rounded text-[10.5px] font-medium ${
                  activeCategoryFilter === cat ? "bg-amber-500 text-slate-950 font-bold" : "bg-slate-900 text-slate-400 hover:text-white"
                }`}
              >
                {cat}
              </button>
            ))}
          </div>

          <div className="bg-slate-900/60 border border-white/5 rounded-xl overflow-hidden shadow">
            <div className="bg-slate-950/30 border-b border-light/5 px-4 py-2.5 text-[10px] font-mono text-slate-500 uppercase tracking-widest grid grid-cols-12 gap-2 font-bold select-none text-left">
              <span className="col-span-3">Member name</span>
              <span className="col-span-3">Contact Email</span>
              <span className="col-span-2 text-center">Category</span>
              <span className="col-span-3 text-center">Assigned Circle Group</span>
              <span className="col-span-1 text-right">Location</span>
            </div>

            <div className="divide-y divide-white/5">
              {filteredMembers.map(m => (
                <div key={m.id} className="px-4 py-3.5 grid grid-cols-12 gap-2 text-xs items-center text-left hover:bg-slate-950/10">
                  <p className="col-span-3 font-semibold text-slate-100 truncate">{m.name}</p>
                  <p className="col-span-3 text-slate-400 font-mono truncate">{m.email}</p>
                  <div className="col-span-2 text-center">
                    <span className="inline-block text-[9.5px] font-semibold text-amber-400 bg-amber-500/5 px-2 py-0.5 rounded border border-amber-500/10">
                      {m.category}
                    </span>
                  </div>
                  <p className="col-span-3 text-center text-[10.5px] text-slate-350 truncate">{m.activeGroup}</p>
                  <p className="col-span-1 text-right text-slate-500 font-mono truncate">{m.location.split(",")[0]}</p>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* 2. WORKSHOPS EVENING TICKETING */}
      {activeSegment === "events" && (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {events.map(ev => (
            <div key={ev.id} className="bg-slate-900 border border-slate-800 rounded-xl p-5 space-y-4 shadow flex flex-col justify-between text-xs">
              <div className="space-y-2">
                <div className="flex justify-between items-start">
                  <span className="text-[9.5px] bg-slate-950 text-slate-300 font-mono px-2 py-0.5 rounded border border-white/5 font-semibold uppercase">
                    {ev.venueType}
                  </span>
                  <span className="text-amber-400 font-semibold font-mono text-[11px]">INR {ev.ticketPrice.toLocaleString()} / seat</span>
                </div>
                <h4 className="font-bold text-slate-200 text-sm leading-snug">{ev.title}</h4>
                <p className="text-slate-450 leading-relaxed text-[11px] font-sans pb-1">{ev.description}</p>
              </div>

              <div className="space-y-1 bg-slate-950/30 p-3 rounded font-mono text-[10px] text-slate-400">
                <p className="flex items-center gap-1.5"><Clock className="w-3.5 h-3.5 shrink-0" /> {new Date(ev.dateTime).toLocaleString([], { dateStyle: "short", timeStyle: "short" })}</p>
                <p className="flex items-center gap-1.5"><MapPin className="w-3.5 h-3.5 shrink-0" /> <span className="truncate">{ev.venueDetails}</span></p>
              </div>

              <div className="border-t border-white/5 pt-3 flex justify-between items-center text-[10.5px]">
                <span className="text-slate-500">Seats Reserved: <span className="font-bold text-slate-300 font-sans">{ev.seatsBooked} / {ev.totalSeats}</span></span>
                <button
                  onClick={() => setActiveEventTicket(ev)}
                  disabled={ev.seatsBooked >= ev.totalSeats}
                  className="bg-amber-500 hover:bg-amber-450 text-slate-950 font-bold px-3 py-1.5 rounded transition-all text-[10px] cursor-pointer disabled:opacity-50"
                >
                  {ev.seatsBooked >= ev.totalSeats ? "Sold out" : "Buy Ticket"}
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* 3. SOCIAL MEDIA CAMPAIGNS CALENDAR */}
      {activeSegment === "social" && (
        <div className="space-y-6">
          {/* Calendar planner summary */}
          <div className="bg-slate-900 border border-white/5 rounded-xl p-5 shadow">
            <h3 className="font-bold text-sm text-slate-200 mb-4 flex items-center gap-1.5">
              <Share2 className="w-4 h-4 text-purple-400" />
              <span>Broadcast Content Calendar</span>
            </h3>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              {socialPosts.map(post => (
                <div key={post.id} className="bg-slate-950/40 p-4 rounded-xl border border-white/5 hover:border-slate-800 transition-all flex flex-col justify-between h-44 text-xs">
                  <div className="space-y-2">
                    <div className="flex justify-between items-center">
                      <span className="text-[10px] font-mono uppercase bg-slate-850 px-2 py-0.5 rounded text-amber-400 font-bold">
                        {post.platform}
                      </span>
                      <span className="text-[9.5px] text-slate-500 font-mono">{post.scheduledDate}</span>
                    </div>
                    <p className="font-semibold text-slate-200 leading-snug line-clamp-2">{post.title}</p>
                    <p className="text-[10px] text-slate-500 font-mono">Campaign: {post.campaignName}</p>
                  </div>

                  {/* analytics metrics */}
                  <div className="border-t border-white/5 pt-2.5 mt-2.5 flex justify-between items-center font-mono text-[10px]">
                    <span className="text-slate-500">Clicks: <span className="text-slate-350">{post.clicks}</span></span>
                    <span className="text-emerald-400 font-semibold">Leads: {post.leadsGenerated}</span>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* TICKET RESERVATIONS DIALOG MODAL */}
      {activeEventTicket && (
        <div className="fixed inset-0 bg-slate-950/80 backdrop-blur-xs flex items-center justify-center z-50 p-4">
          <div className="bg-slate-900 border border-amber-500/15 rounded-xl w-full max-w-sm overflow-hidden text-xs">
            <div className="p-4 bg-slate-950/65 border-b border-light/5 flex justify-between items-center">
              <h3 className="font-bold text-slate-100 text-sm">Reserves Entry Ticket</h3>
              <button onClick={() => setActiveEventTicket(null)} className="text-slate-400 p-1 hover:text-white rounded">✕</button>
            </div>

            <form onSubmit={submitTicketBooking} className="p-5 space-y-4">
              <div>
                <label className="text-[10px] font-mono text-slate-400 block mb-1">Event Target Context</label>
                <div className="p-2 bg-slate-950 rounded text-slate-250 font-bold border border-white/5">
                  {activeEventTicket.title}
                </div>
              </div>

              <div>
                <label className="text-[10px] font-mono text-slate-400 block mb-1">Registrar Name *</label>
                <input
                  type="text"
                  required
                  placeholder="Kalyan"
                  value={ticketForm.memberName}
                  onChange={e => setTicketForm({ ...ticketForm, memberName: e.target.value })}
                  className="w-full bg-slate-955 border border-slate-800 rounded px-2.5 py-1.5 text-white"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="text-[10px] font-mono text-slate-400 block mb-1">Registrar Email *</label>
                  <input
                    type="email"
                    required
                    placeholder="kalyan@gmail.com"
                    value={ticketForm.memberEmail}
                    onChange={e => setTicketForm({ ...ticketForm, memberEmail: e.target.value })}
                    className="w-full bg-slate-955 border border-slate-800 rounded px-2.5 py-1.5 text-white"
                  />
                </div>
                <div>
                  <label className="text-[10px] font-mono text-slate-400 block mb-1">Ticket Count *</label>
                  <input
                    type="number"
                    required
                    min="1"
                    value={ticketForm.ticketCount}
                    onChange={e => setTicketForm({ ...ticketForm, ticketCount: Number(e.target.value) })}
                    className="w-full bg-slate-955 border border-slate-800 rounded px-2.5 py-1.5 text-white"
                  />
                </div>
              </div>

              <div className="border-t border-white/5 pt-3.5 italic text-slate-500 text-[10px]">
                Grand total price: <span className="font-bold text-slate-300 font-sans">INR {(activeEventTicket.ticketPrice * ticketForm.ticketCount).toLocaleString()}</span>
              </div>

              <div className="flex justify-end gap-2 text-xs font-bold pt-2">
                <button
                  type="button"
                  onClick={() => setActiveEventTicket(null)}
                  className="bg-slate-800 hover:bg-slate-755 text-slate-350 border border-slate-705 px-4 py-2 rounded-lg cursor-pointer"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="bg-amber-500 hover:bg-amber-450 text-slate-950 px-4 py-2 rounded-lg shadow-lg shadow-amber-500/10 cursor-pointer animate-pulse"
                >
                  Purchase Entry
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* CREATE MEMBER MODAL */}
      {isOpenAddM && (
        <div className="fixed inset-0 bg-slate-950/80 backdrop-blur-xs flex items-center justify-center z-50 p-4">
          <div className="bg-slate-900 border border-amber-500/15 rounded-xl w-full max-w-sm overflow-hidden text-xs">
            <div className="p-4 bg-slate-950/65 border-b border-white/5 flex justify-between items-center">
              <h3 className="font-bold text-slate-100 text-sm">Add Community Member</h3>
              <button onClick={() => setIsOpenAddM(false)} className="text-slate-400 p-1 hover:text-white rounded">✕</button>
            </div>

            <form onSubmit={submitMember} className="p-5 space-y-4">
              <div>
                <label className="text-[10px] font-mono text-slate-400 block mb-1">Legal Name *</label>
                <input
                  type="text"
                  required
                  placeholder="Kalyan"
                  value={newM.name}
                  onChange={e => setNewM({ ...newM, name: e.target.value })}
                  className="w-full bg-slate-955 border border-slate-800 rounded px-2.5 py-1.5 text-white"
                />
              </div>
              <div>
                <label className="text-[10px] font-mono text-slate-400 block mb-1">Primary Email *</label>
                <input
                  type="email"
                  required
                  placeholder="kalyan@gmail.com"
                  value={newM.email}
                  onChange={e => setNewM({ ...newM, email: e.target.value })}
                  className="w-full bg-slate-955 border border-slate-800 rounded px-2.5 py-1.5 text-white"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="text-[10px] font-mono text-slate-400 block mb-1">Select Guild Group *</label>
                  <select
                    value={newM.category}
                    onChange={e => setNewM({ ...newM, category: e.target.value as any })}
                    className="w-full bg-slate-955 border border-slate-800 rounded px-2 py-1.5 text-white cursor-pointer"
                  >
                    {categories.map(c => (
                      <option key={c} value={c} className="bg-slate-900">{c}</option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="text-[10px] font-mono text-slate-400 block mb-1">Regional Location</label>
                  <input
                    type="text"
                    value={newM.location}
                    onChange={e => setNewM({ ...newM, location: e.target.value })}
                    className="w-full bg-slate-955 border border-slate-800 rounded px-2.5 py-1.5 text-white"
                  />
                </div>
              </div>

              <div>
                <label className="text-[10px] font-mono text-slate-400 block mb-1">Assigned Operational Circle Unit</label>
                <input
                  type="text"
                  value={newM.activeGroup}
                  onChange={e => setNewM({ ...newM, activeGroup: e.target.value })}
                  className="w-full bg-slate-955 border border-slate-800 rounded px-2.5 py-1.5 text-white"
                />
              </div>

              <div className="pt-2 flex justify-end gap-2 text-xs font-bold">
                <button
                  type="button"
                  onClick={() => setIsOpenAddM(false)}
                  className="bg-slate-800 hover:bg-slate-755 text-slate-350 border border-slate-705 px-4 py-2 rounded-lg cursor-pointer"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="bg-amber-500 hover:bg-amber-450 text-slate-950 px-4 py-2 rounded-lg shadow-lg shadow-amber-500/10 cursor-pointer"
                >
                  Index Member
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* CREATE POST SCHEDULER MODAL */}
      {isOpenAddPost && (
        <div className="fixed inset-0 bg-slate-955/85 backdrop-blur-xs flex items-center justify-center z-50 p-4">
          <div className="bg-slate-900 border border-amber-500/15 rounded-xl w-full max-w-sm overflow-hidden text-xs">
            <div className="p-4 bg-slate-950/65 border-b border-light/5 flex justify-between items-center">
              <h3 className="font-bold text-slate-100 text-sm">Schedule social broadcast post</h3>
              <button onClick={() => setIsOpenAddPost(false)} className="text-slate-400 p-1 hover:text-white rounded">✕</button>
            </div>

            <form onSubmit={submitPost} className="p-5 space-y-4">
              <div>
                <label className="text-[10px] font-mono text-slate-400 block mb-1">Content Title Particulars *</label>
                <textarea
                  required
                  rows={2}
                  placeholder="e.g. Venus Remedies timing on ALP systems..."
                  value={newPost.title}
                  onChange={e => setNewPost({ ...newPost, title: e.target.value })}
                  className="w-full bg-slate-955 border border-slate-800 rounded p-2 text-white"
                />
              </div>

              <div className="grid grid-cols-2 gap-3.5">
                <div>
                  <label className="text-[10px] font-mono text-slate-400 block mb-1">Target Media Platform *</label>
                  <select
                    value={newPost.platform}
                    onChange={e => setNewPost({ ...newPost, platform: e.target.value as any })}
                    className="w-full bg-slate-955 border border-slate-800 rounded px-2 py-1.5 text-white cursor-pointer"
                  >
                    {platforms.map(p => (
                      <option key={p} value={p} className="bg-slate-900">{p}</option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="text-[10px] font-mono text-slate-400 block mb-1">Scheduled Release Date *</label>
                  <input
                    type="date"
                    required
                    value={newPost.scheduledDate}
                    onChange={e => setNewPost({ ...newPost, scheduledDate: e.target.value })}
                    className="w-full bg-slate-955 border border-slate-800 rounded px-2.5 py-1.5 text-white cursor-pointer"
                  />
                </div>
              </div>

              <div>
                <label className="text-[10px] font-mono text-slate-400 block mb-1">Campaign Association Title</label>
                <input
                  type="text"
                  value={newPost.campaignName}
                  onChange={e => setNewPost({ ...newPost, campaignName: e.target.value })}
                  className="w-full bg-slate-955 border border-slate-800 rounded px-2.5 py-1.5 text-white"
                />
              </div>

              <div className="pt-2 flex justify-end gap-2 text-xs font-bold">
                <button
                  type="button"
                  onClick={() => setIsOpenAddPost(false)}
                  className="bg-slate-800 hover:bg-slate-755 text-slate-350 border border-slate-705 px-4 py-2 rounded-lg cursor-pointer"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="bg-amber-500 hover:bg-amber-450 text-slate-950 px-4 py-2 rounded-lg shadow-lg shadow-amber-500/10 cursor-pointer"
                >
                  Confirm Schedule
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
