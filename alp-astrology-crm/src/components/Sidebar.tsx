import React from "react";
import {
  LayoutDashboard,
  Users2,
  CalendarDays,
  GraduationCap,
  Sparkles,
  CreditCard,
  Award,
  Share2,
  FileBarChart2,
  DraftingCompass,
  UserCheck,
  Power,
  ChevronDown
} from "lucide-react";
import { User, UserRole } from "../types";

interface SidebarProps {
  currentUser: User;
  onSwitchRole: (role: UserRole) => void;
  activeTab: string;
  setActiveTab: (tab: string) => void;
}

export function Sidebar({ currentUser, onSwitchRole, activeTab, setActiveTab }: SidebarProps) {
  const [dropdownOpen, setDropdownOpen] = React.useState(false);

  const roles: UserRole[] = [
    "Super Admin",
    "Admin",
    "Trainer",
    "Astrologer",
    "Receptionist",
    "Social Media Manager",
    "Account Manager",
    "Student",
    "Community Member"
  ];

  // Configure RBAC navigation permissions
  const menuItems = [
    { id: "dashboard", label: "Dashboard", icon: LayoutDashboard, roles: ["all"] },
    { id: "leads", label: "Leads CRM", icon: Users2, roles: ["Super Admin", "Admin", "Astrologer", "Receptionist", "Social Media Manager"] },
    { id: "consultations", label: "Consultations", icon: Sparkles, roles: ["Super Admin", "Admin", "Astrologer", "Receptionist"] },
    { id: "courses", label: "Course Batches", icon: GraduationCap, roles: ["Super Admin", "Admin", "Trainer", "Student"] },
    { id: "students", label: "Students", icon: UserCheck, roles: ["Super Admin", "Admin", "Trainer"] },
    { id: "payments", label: "Finances & Bills", icon: CreditCard, roles: ["Super Admin", "Admin", "Receptionist", "Account Manager"] },
    { id: "certificates", label: "Certificates", icon: Award, roles: ["Super Admin", "Admin", "Trainer", "Student"] },
    { id: "community", label: "Community", icon: Users2, roles: ["Super Admin", "Admin", "Trainer", "Community Member"] },
    { id: "events", label: "Events Manager", icon: CalendarDays, roles: ["Super Admin", "Admin", "Receptionist", "Community Member"] },
    { id: "social", label: "Social Media CRM", icon: Share2, roles: ["Super Admin", "Admin", "Social Media Manager"] },
    { id: "employees", label: "Co-workers Registry", icon: UserCheck, roles: ["Super Admin", "Admin"] },
    { id: "reports", label: "Reports & Exports", icon: FileBarChart2, roles: ["Super Admin", "Admin"] },
    { id: "blueprint", label: "Blueprint Hub", icon: DraftingCompass, roles: ["all"] }
  ];

  const filteredItems = menuItems.filter(
    item => item.roles.includes("all") || item.roles.includes(currentUser.role)
  );

  return (
    <aside className="w-64 bg-slate-900 border-r border-slate-800 flex flex-col h-screen overflow-hidden shrink-0">
      {/* Brand Header */}
      <div className="p-4 border-b border-slate-800 flex flex-col gap-1">
        <div className="flex items-center gap-2.5">
          <div className="w-9 h-9 bg-amber-500 rounded-lg flex items-center justify-center font-bold text-white text-lg shadow-sm shadow-amber-500/20">
            <span className="tracking-tighter">ALP</span>
          </div>
          <div>
            <h1 className="text-white font-bold text-sm tracking-tight leading-none uppercase">ALP Astrology</h1>
            <p className="text-slate-400 text-[10px] uppercase mt-1 tracking-wider">Enterprise CRM</p>
          </div>
        </div>
      </div>

      {/* Role Switcher Drawer */}
      <div className="p-3 border-b border-slate-800 relative bg-slate-950/20">
        <label className="text-[9px] text-amber-505 font-bold uppercase tracking-widest font-mono block mb-1">
          Active UI Context Role
        </label>
        <button
          onClick={() => setDropdownOpen(!dropdownOpen)}
          className="w-full flex items-center justify-between bg-slate-800 hover:bg-slate-750 text-white rounded-md px-2.5 py-1.5 text-xs border border-slate-700 font-medium transition-all"
        >
          <span className="truncate">{currentUser.role}</span>
          <ChevronDown className="w-3 h-3 text-slate-400 ml-1 shrink-0" />
        </button>

        {dropdownOpen && (
          <div className="absolute top-[calc(100%-8px)] left-3 right-3 bg-slate-850 border border-slate-700 rounded-md py-1 shadow-2xl z-50 max-h-56 overflow-y-auto">
            {roles.map(r => (
              <button
                key={r}
                onClick={() => {
                  onSwitchRole(r);
                  setDropdownOpen(false);
                }}
                className={`w-full text-left px-3 py-1.5 text-xs hover:bg-amber-500 hover:text-slate-950 transition-colors ${
                  currentUser.role === r ? "bg-amber-500/10 text-amber-400 font-bold" : "text-slate-300"
                }`}
              >
                {r}
              </button>
            ))}
          </div>
        )}
      </div>

      {/* Navigation Links */}
      <nav className="flex-1 overflow-y-auto p-2 space-y-0.5">
        {filteredItems.map(item => {
          const Icon = item.icon;
          const isActive = activeTab === item.id;
          return (
            <button
              key={item.id}
              onClick={() => setActiveTab(item.id)}
              className={`w-full flex items-center gap-3 px-3 py-2 rounded-md text-xs transition-colors cursor-pointer ${
                isActive
                  ? "bg-amber-500/10 text-amber-500 font-semibold"
                  : "text-slate-400 hover:bg-slate-800 hover:text-white"
              }`}
            >
              <Icon className={`w-3.5 h-3.5 shrink-0 ${isActive ? "text-amber-500" : "text-slate-400"}`} />
              <span>{item.label}</span>
            </button>
          );
        })}
      </nav>

      {/* Footer Profile */}
      <div className="p-3 border-t border-slate-800 bg-slate-950/20 flex items-center gap-2.5">
        <img
          src={currentUser.avatar || "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=100"}
          alt="Avatar"
          className="w-8 h-8 rounded-full border border-slate-700 object-cover bg-slate-850"
        />
        <div className="min-w-0">
          <p className="text-xs font-semibold text-slate-100 truncate">{currentUser.name}</p>
          <div className="flex items-center gap-1.5 mt-0.5">
            <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse"></span>
            <span className="text-[9px] text-slate-400 uppercase font-mono tracking-wider">REST Session Live</span>
          </div>
        </div>
      </div>
    </aside>
  );
}
