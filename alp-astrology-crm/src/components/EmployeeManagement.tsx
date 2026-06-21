import React from "react";
import {
  UserPlus,
  Lock,
  Mail,
  Phone,
  Trash2,
  Edit2,
  Eye,
  EyeOff,
  UserCheck,
  ShieldCheck,
  Zap,
  Info,
  RefreshCw,
  Search,
  CheckCircle,
  XCircle,
  Clock
} from "lucide-react";
import { Employee, UserRole } from "../types";

interface EmployeeManagementProps {
  currentUserId: string;
  currentUserRole: string;
  showNotification: (channel: string, message: string, status: "success" | "danger" | "info") => void;
}

export function EmployeeManagement({
  currentUserId,
  currentUserRole,
  showNotification
}: EmployeeManagementProps) {
  const [employees, setEmployees] = React.useState<Employee[]>([]);
  const [searchQuery, setSearchQuery] = React.useState("");
  const [isLoading, setIsLoading] = React.useState(true);
  const [revealPasswords, setRevealPasswords] = React.useState<{ [id: string]: boolean }>({});

  // Form states
  const [showAddForm, setShowAddForm] = React.useState(false);
  const [name, setName] = React.useState("");
  const [email, setEmail] = React.useState("");
  const [password, setPassword] = React.useState("");
  const [role, setRole] = React.useState<UserRole>("Astrologer");
  const [phone, setPhone] = React.useState("");
  const [status, setStatus] = React.useState<"Active" | "Suspended" | "On Leave">("Active");
  const [avatar, setAvatar] = React.useState("");
  const [isSubmitting, setIsSubmitting] = React.useState(false);

  // Edit states
  const [editingEmployee, setEditingEmployee] = React.useState<Employee | null>(null);

  // Load employees from server
  const fetchEmployees = () => {
    setIsLoading(true);
    fetch("/api/employees")
      .then((res) => res.json())
      .then((data) => {
        setEmployees(data || []);
      })
      .catch((err) => {
        console.error("Failed to load coworkers list", err);
        showNotification("Security Logs", "Failed to retrieve co-worker directory.", "danger");
      })
      .finally(() => {
        setIsLoading(false);
      });
  };

  React.useEffect(() => {
    fetchEmployees();
  }, []);

  const handleCreateEmployee = (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim() || !email.trim() || !password || !role) {
      showNotification("Validation", "Check mandatory inputs: Name, Email, Password & Role.", "danger");
      return;
    }

    setIsSubmitting(true);
    const payload = {
      name: name.trim(),
      email: email.trim(),
      password,
      role,
      phone: phone.trim(),
      avatar: avatar.trim() || undefined,
      status
    };

    fetch("/api/employees", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload)
    })
      .then(async (res) => {
        const data = await res.json();
        if (!res.ok) {
          throw new Error(data.error || "Co-worker creation failed");
        }
        return data;
      })
      .then((newEmp) => {
        showNotification("Access Security", `Successfully registered coworker: ${newEmp.name} as ${newEmp.role}`, "success");
        fetchEmployees();
        // Reset Form
        setName("");
        setEmail("");
        setPassword("");
        setRole("Astrologer");
        setPhone("");
        setAvatar("");
        setStatus("Active");
        setShowAddForm(false);
      })
      .catch((err) => {
        showNotification("Security Error", err.message, "danger");
      })
      .finally(() => {
        setIsSubmitting(false);
      });
  };

  const handleUpdateEmployee = (e: React.FormEvent) => {
    e.preventDefault();
    if (!editingEmployee) return;

    setIsSubmitting(true);
    fetch(`/api/employees/${editingEmployee.id}`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(editingEmployee)
    })
      .then(async (res) => {
        const data = await res.json();
        if (!res.ok) {
          throw new Error(data.error || "Update operation failed");
        }
        return data;
      })
      .then((updated) => {
        showNotification("Access Security", `Updated profile of co-worker: ${updated.name}`, "success");
        fetchEmployees();
        setEditingEmployee(null);
      })
      .catch((err) => {
        showNotification("Security Error", err.message, "danger");
      })
      .finally(() => {
        setIsSubmitting(false);
      });
  };

  const handleDeleteEmployee = (id: string, name: string) => {
    if (id === currentUserId) {
      showNotification("Security Action", "Access Denied: You cannot delete your own logged-in account.", "danger");
      return;
    }

    if (id === "emp-1") {
      showNotification("Security Action", "Access Denied: The root Founder account has absolute immunity.", "danger");
      return;
    }

    if (!confirm(`Are you absolutely sure you want to remove co-worker "${name}" from ALP Astrology CRM?`)) {
      return;
    }

    fetch(`/api/employees/${id}`, {
      method: "DELETE"
    })
      .then(async (res) => {
        const data = await res.json();
        if (!res.ok) {
          throw new Error(data.error || "Delete operation failed");
        }
        return data;
      })
      .then(() => {
        showNotification("Access Security", `Removed co-worker account "${name}" from central indexing.`, "success");
        fetchEmployees();
      })
      .catch((err) => {
        showNotification("Security Error", err.message, "danger");
      });
  };

  const handleToggleStatus = (emp: Employee) => {
    if (emp.id === "emp-1" || emp.id === currentUserId) {
      showNotification("Security Access", "Cannot toggle status of the primary administrative account.", "danger");
      return;
    }

    const nextStatus = emp.status === "Active" ? "Suspended" : "Active";
    
    fetch(`/api/employees/${emp.id}`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ status: nextStatus })
    })
      .then(async (res) => {
        const data = await res.json();
        if (!res.ok) {
          throw new Error(data.error || "Status toggle failed");
        }
        return data;
      })
      .then(() => {
        showNotification("Access Change", `Updated coworker ${emp.name} account status to ${nextStatus}`, "info");
        fetchEmployees();
      })
      .catch((err) => {
        showNotification("Access Error", err.message, "danger");
      });
  };

  const togglePasswordReveal = (id: string) => {
    setRevealPasswords(prev => ({
      ...prev,
      [id]: !prev[id]
    }));
  };

  const filteredEmployees = employees.filter((emp) => {
    const q = searchQuery.toLowerCase().trim();
    if (!q) return true;
    return (
      emp.name.toLowerCase().includes(q) ||
      emp.email.toLowerCase().includes(q) ||
      emp.role.toLowerCase().includes(q) ||
      (emp.phone && emp.phone.includes(q))
    );
  });

  const availableRoles: UserRole[] = [
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

  const getStatusBadge = (s: Employee["status"]) => {
    switch (s) {
      case "Active":
        return (
          <span className="flex items-center gap-1 text-[9.5px] font-extrabold px-2 py-0.5 rounded-full bg-emerald-50 text-emerald-700 border border-emerald-200">
            <CheckCircle className="w-2.5 h-2.5" />
            <span>ACTIVE ACCESS</span>
          </span>
        );
      case "Suspended":
        return (
          <span className="flex items-center gap-1 text-[9.5px] font-extrabold px-2 py-0.5 rounded-full bg-rose-50 text-rose-700 border border-rose-200">
            <XCircle className="w-2.5 h-2.5" />
            <span>REVOKED (SUSPENDED)</span>
          </span>
        );
      case "On Leave":
        return (
          <span className="flex items-center gap-1 text-[9.5px] font-extrabold px-2 py-0.5 rounded-full bg-amber-50 text-amber-700 border border-amber-200">
            <Clock className="w-2.5 h-2.5" />
            <span>ON LEAVE / AWAY</span>
          </span>
        );
    }
  };

  const getRoleBadgeColor = (r: UserRole) => {
    if (r === "Super Admin" || r === "Admin") return "bg-purple-100 text-purple-800 border-purple-200";
    if (r === "Astrologer") return "bg-amber-100 text-amber-800 border-amber-200";
    if (r === "Trainer") return "bg-blue-100 text-blue-800 border-blue-200";
    if (r === "Receptionist") return "bg-rose-100 text-rose-800 border-rose-200";
    return "bg-slate-100 text-slate-800 border-slate-200";
  };

  // Check permissions (Super Admin and Admin can view/modify)
  const isPrivileged = currentUserRole === "Super Admin" || currentUserRole === "Admin";

  if (!isPrivileged) {
    return (
      <div className="bg-white border border-slate-200 rounded-xl p-8 text-center max-w-xl mx-auto my-12 space-y-4">
        <div className="w-12 h-12 bg-rose-50 border border-rose-200 rounded-xl flex items-center justify-center text-rose-600 mx-auto">
          <Lock className="w-6 h-6" />
        </div>
        <h2 className="text-sm font-bold text-slate-800 uppercase tracking-widest">Privilege Access Restricted</h2>
        <p className="text-xs text-slate-500 leading-relaxed">
          The co-worker credentials and role provisioning database is only accessible to administrative specialists (Super Admin or Admin co-workers). Please authenticate under a credential with high authority clearance level to inspect credentials.
        </p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header section */}
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4 bg-slate-905 p-1 border-b border-slate-100 pb-4">
        <div>
          <span className="text-[10px] uppercase font-bold text-amber-600 tracking-widest font-mono">
            ALP Astrology Workspace Team
          </span>
          <h2 className="text-lg font-black text-slate-800 uppercase tracking-tight">
            Co-worker & Staff Access Keys
          </h2>
          <p className="text-xs text-slate-500">
            Manage employee profiles, login credentials, and assign permission authorization.
          </p>
        </div>

        <button
          onClick={() => {
            setShowAddForm(!showAddForm);
            setEditingEmployee(null);
          }}
          className="bg-amber-500 hover:bg-amber-600 font-extrabold text-slate-950 px-4 py-2 rounded-xl text-xs flex items-center gap-1.5 shadow-sm cursor-pointer transition-all shrink-0"
        >
          <UserPlus className="w-4 h-4 text-slate-950" />
          <span>{showAddForm ? "Hide Register Form" : "Register New Employee"}</span>
        </button>
      </div>

      {/* Quick Access credentials reference box */}
      <div className="bg-amber-50/50 border border-amber-500/20 rounded-xl p-4 flex flex-col md:flex-row gap-4 items-start md:items-center justify-between text-xs text-slate-700">
        <div className="flex gap-2.5 items-start">
          <Info className="w-5 h-5 text-amber-600 shrink-0 mt-0.5 animate-pulse" />
          <div className="space-y-0.5">
            <span className="font-bold text-amber-800 uppercase tracking-wider block text-[10.5px]">Simulation Instruction</span>
            <p className="text-[11px] leading-relaxed max-w-2xl">
              You can easily log in as any created Employee from the <strong>Top Right Header Auth Panel</strong>! Create any custom email & password here, then sign in with it to test specific workflow permissions.
            </p>
          </div>
        </div>
        <button
          onClick={fetchEmployees}
          className="flex items-center gap-1 text-[11px] px-2.5 py-1 text-amber-800 bg-white hover:bg-amber-100 border border-amber-500/10 rounded-lg cursor-pointer transition-colors"
        >
          <RefreshCw className="w-3 h-3" />
          <span>Synchronize Grid</span>
        </button>
      </div>

      {/* Register Form */}
      {showAddForm && (
        <form
          onSubmit={handleCreateEmployee}
          className="bg-white border-2 border-amber-500/30 rounded-xl p-5 shadow-xs space-y-4 animate-in fade-in slide-in-from-top duration-300"
        >
          <div className="flex items-center justify-between border-b border-slate-100 pb-2">
            <h3 className="font-extrabold text-xs text-slate-800 uppercase tracking-wider flex items-center gap-1.5">
              <Zap className="w-4 h-4 text-amber-500" />
              <span>Provision Coworker Access</span>
            </h3>
            <span className="text-[10px] text-slate-400 font-mono">Create custom coworker login keys</span>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div>
              <label className="block text-[10px] font-bold text-slate-600 uppercase mb-1">Worker Full Name *</label>
              <input
                type="text"
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="e.g. Employee 1"
                required
                className="w-full bg-slate-50 border border-slate-200 rounded-lg px-3 py-2 text-xs text-slate-800 placeholder-slate-400 focus:outline-hidden focus:border-amber-500"
              />
            </div>

            <div>
              <label className="block text-[10px] font-bold text-slate-600 uppercase mb-1">User Email Address (Login User ID) *</label>
              <div className="relative">
                <Mail className="absolute left-3 top-2.5 w-3.5 h-3.5 text-slate-400" />
                <input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="e.g. employee1@alpastrology.com"
                  required
                  className="w-full bg-slate-50 border border-slate-200 rounded-lg pl-9 pr-3 py-2 text-xs text-slate-800 placeholder-slate-400 focus:outline-hidden focus:border-amber-500"
                />
              </div>
            </div>

            <div>
              <label className="block text-[10px] font-bold text-slate-600 uppercase mb-1">Login Password *</label>
              <div className="relative">
                <Lock className="absolute left-3 top-2.5 w-3.5 h-3.5 text-slate-400" />
                <input
                  type="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="e.g. employee1"
                  required
                  className="w-full bg-slate-50 border border-slate-200 rounded-lg pl-9 pr-3 py-2 text-xs text-slate-800 placeholder-slate-400 focus:outline-hidden focus:border-amber-500"
                />
              </div>
            </div>

            <div>
              <label className="block text-[10px] font-bold text-slate-600 uppercase mb-1">Assigned Role *</label>
              <select
                value={role}
                onChange={(e) => setRole(e.target.value as UserRole)}
                className="w-full bg-slate-50 border border-slate-200 rounded-lg px-3 py-2 text-xs text-slate-700 cursor-pointer focus:outline-hidden focus:border-amber-500"
              >
                {availableRoles.filter(r => r !== "Student" && r !== "Community Member").map((r) => (
                  <option key={r} value={r}>
                    {r}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-[10px] font-bold text-slate-600 uppercase mb-1">Mobile Contact No</label>
              <div className="relative">
                <Phone className="absolute left-3 top-2.5 w-3.5 h-3.5 text-slate-400" />
                <input
                  type="tel"
                  value={phone}
                  onChange={(e) => setPhone(e.target.value)}
                  placeholder="e.g. +91 99999 88888"
                  className="w-full bg-slate-50 border border-slate-200 rounded-lg pl-9 pr-3 py-2 text-xs text-slate-800 placeholder-slate-400 focus:outline-hidden focus:border-amber-500"
                />
              </div>
            </div>

            <div>
              <label className="block text-[10px] font-bold text-slate-600 uppercase mb-1">Status Authorization</label>
              <select
                value={status}
                onChange={(e: any) => setStatus(e.target.value)}
                className="w-full bg-slate-50 border border-slate-200 rounded-lg px-3 py-2 text-xs text-slate-700 cursor-pointer focus:outline-hidden focus:border-amber-500"
              >
                <option value="Active">Active</option>
                <option value="Suspended">Suspended</option>
                <option value="On Leave">On Leave</option>
              </select>
            </div>

            <div className="md:col-span-3">
              <label className="block text-[10px] font-bold text-slate-600 uppercase mb-1">Avatar Image Url</label>
              <input
                type="url"
                value={avatar}
                onChange={(e) => setAvatar(e.target.value)}
                placeholder="e.g. https://images.unsplash.com/... or leave blank for automatic"
                className="w-full bg-slate-50 border border-slate-200 rounded-lg px-3 py-2 text-xs text-slate-800 placeholder-slate-400 focus:outline-hidden focus:border-amber-500"
              />
            </div>
          </div>

          <div className="flex justify-end gap-2 border-t border-slate-100 pt-3">
            <button
              type="button"
              onClick={() => setShowAddForm(false)}
              className="border border-slate-200 hover:bg-slate-50 text-slate-600 px-4 py-2 rounded-lg text-xs font-bold font-mono uppercase cursor-pointer"
            >
              Discard
            </button>
            <button
              type="submit"
              disabled={isSubmitting}
              className="bg-amber-500 hover:bg-amber-600 disabled:opacity-50 text-slate-950 px-5 py-2 rounded-lg text-xs font-extrabold flex items-center gap-1 cursor-pointer font-mono uppercase"
            >
              {isSubmitting ? "Processing..." : "Enroll Employee Access"}
            </button>
          </div>
        </form>
      )}

      {/* Editing Form */}
      {editingEmployee && (
        <form
          onSubmit={handleUpdateEmployee}
          className="bg-slate-50 border-2 border-blue-500/30 rounded-xl p-5 shadow-xs space-y-4 animate-in fade-in duration-300"
        >
          <div className="flex items-center justify-between border-b border-slate-200 pb-2">
            <h3 className="font-extrabold text-xs text-blue-850 uppercase tracking-wider flex items-center gap-1.5">
              <Edit2 className="w-4 h-4 text-blue-500" />
              <span>Modify Employee profile: {editingEmployee.name}</span>
            </h3>
            <button
              type="button"
              onClick={() => setEditingEmployee(null)}
              className="text-slate-400 hover:text-slate-600 text-[11px] font-bold font-mono"
            >
              [X] CLOSE
            </button>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div>
              <label className="block text-[10px] font-bold text-slate-600 uppercase mb-1">Name</label>
              <input
                type="text"
                value={editingEmployee.name}
                onChange={(e) => setEditingEmployee({ ...editingEmployee, name: e.target.value })}
                required
                className="w-full bg-white border border-slate-250 rounded-lg px-3 py-2 text-xs text-slate-800 placeholder-slate-400 focus:outline-hidden"
              />
            </div>

            <div>
              <label className="block text-[10px] font-bold text-slate-600 uppercase mb-1">Email</label>
              <input
                type="email"
                value={editingEmployee.email}
                onChange={(e) => setEditingEmployee({ ...editingEmployee, email: e.target.value })}
                required
                className="w-full bg-white border border-slate-250 rounded-lg px-3 py-2 text-xs text-slate-800 placeholder-slate-400 focus:outline-hidden"
              />
            </div>

            <div>
              <label className="block text-[10px] font-bold text-slate-600 uppercase mb-1">Password</label>
              <input
                type="text"
                value={editingEmployee.password || ""}
                onChange={(e) => setEditingEmployee({ ...editingEmployee, password: e.target.value })}
                required
                className="w-full bg-white border border-slate-250 rounded-lg px-3 py-2 text-xs text-slate-800 placeholder-slate-400 focus:outline-hidden"
              />
            </div>

            <div>
              <label className="block text-[10px] font-bold text-slate-600 uppercase mb-1">Business Role</label>
              <select
                value={editingEmployee.role}
                onChange={(e) => setEditingEmployee({ ...editingEmployee, role: e.target.value as any })}
                className="w-full bg-white border border-slate-250 rounded-lg px-3 py-2 text-xs text-slate-755 cursor-pointer"
              >
                {availableRoles.filter(r => r !== "Student" && r !== "Community Member").map((r) => (
                  <option key={r} value={r}>
                    {r}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-[10px] font-bold text-slate-600 uppercase mb-1">Contact No</label>
              <input
                type="tel"
                value={editingEmployee.phone || ""}
                onChange={(e) => setEditingEmployee({ ...editingEmployee, phone: e.target.value })}
                className="w-full bg-white border border-slate-250 rounded-lg px-3 py-2 text-xs text-slate-800 placeholder-slate-400 focus:outline-hidden whitespace-nowrap"
              />
            </div>

            <div>
              <label className="block text-[10px] font-bold text-slate-600 uppercase mb-1">Office Status</label>
              <select
                value={editingEmployee.status}
                onChange={(e: any) => setEditingEmployee({ ...editingEmployee, status: e.target.value })}
                className="w-full bg-white border border-slate-250 rounded-lg px-3 py-2 text-xs text-slate-755 cursor-pointer"
              >
                <option value="Active">Active</option>
                <option value="Suspended">Suspended</option>
                <option value="On Leave">On Leave</option>
              </select>
            </div>
          </div>

          <div className="flex justify-end gap-2 border-t border-slate-200/80 pt-3">
            <button
              type="button"
              onClick={() => setEditingEmployee(null)}
              className="border border-slate-250 hover:bg-slate-150 text-slate-600 px-4 py-2 rounded-lg text-xs font-bold uppercase tracking-wider"
            >
              Cancel Edit
            </button>
            <button
              type="submit"
              disabled={isSubmitting}
              className="bg-blue-600 hover:bg-blue-700 text-white px-5 py-2 rounded-lg text-xs font-extrabold shadow-sm uppercase font-mono"
            >
              {isSubmitting ? "Saving changes..." : "Save Coworker Details"}
            </button>
          </div>
        </form>
      )}

      {/* Database Search Filter Bar */}
      <div className="flex bg-white items-center gap-2 border border-slate-200 rounded-xl px-3 py-2.5 shadow-2xs max-w-md">
        <Search className="w-4 h-4 text-slate-400 shrink-0" />
        <input
          type="text"
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          placeholder="Search employees by name, email, role or phone..."
          className="bg-transparent text-xs w-full text-slate-800 placeholder-slate-450 focus:outline-hidden"
        />
        {searchQuery && (
          <button
            onClick={() => setSearchQuery("")}
            className="text-slate-400 hover:text-slate-650 text-xs font-mono font-bold uppercase cursor-pointer"
          >
            Clear
          </button>
        )}
      </div>

      {/* Grid List */}
      {isLoading ? (
        <div className="flex flex-col items-center justify-center py-16 text-slate-400 space-y-2">
          <div className="w-8 h-8 border-4 border-amber-500 border-t-transparent rounded-full animate-spin"></div>
          <span className="font-mono text-xs uppercase tracking-widest text-slate-550 animate-pulse">Syncing Co-worker Grid...</span>
        </div>
      ) : filteredEmployees.length === 0 ? (
        <div className="bg-white border border-slate-200 rounded-xl p-12 text-center text-slate-500 space-y-2">
          <span className="text-xl block">👥</span>
          <h4 className="font-bold text-slate-700">No Co-worker matches matching search query</h4>
          <p className="text-xs">Try adjusting your filters, query text, or Register a new employee profile to begin.</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {filteredEmployees.map((emp) => {
            const isRevealed = revealPasswords[emp.id] || false;
            const isSelf = emp.id === currentUserId;
            const isFounder = emp.id === "emp-1";

            return (
              <div
                key={emp.id}
                className={`bg-white border rounded-2xl p-5 relative overflow-hidden transition-all duration-300 hover:shadow-md ${
                  isSelf
                    ? "border-amber-500 ring-1 ring-amber-500/20"
                    : emp.status === "Suspended"
                    ? "border-rose-150 bg-slate-50/50 opacity-90"
                    : "border-slate-200"
                }`}
              >
                {/* ID badge as watermarked label */}
                <div className="absolute top-4 right-4 text-[9px] text-slate-450 font-mono bg-slate-100 rounded px-1.5 py-0.5 border border-slate-200">
                  {emp.id} {isSelf && <span className="text-amber-600 font-extrabold ml-1 uppercase">[YOU/CURRENT SESS]</span>}
                </div>

                <div className="flex items-start gap-4">
                  {/* Left avatar column */}
                  <div className="relative">
                    <img
                      src={emp.avatar || `https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=120`}
                      alt={emp.name}
                      onError={(e) => {
                        // fallback
                        (e.target as HTMLImageElement).src = `https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=120`;
                      }}
                      referrerPolicy="no-referrer"
                      className="w-14 h-14 rounded-full object-cover border border-slate-300 bg-slate-100 shadow-2xs"
                    />
                    <div className="absolute -bottom-1 -right-1">
                      {emp.status === "Active" ? (
                        <div className="w-4.5 h-4.5 bg-emerald-500 border-2 border-white rounded-full flex items-center justify-center text-[8px] text-white font-bold" title="Access live auth enabled">
                          ✓
                        </div>
                      ) : emp.status === "Suspended" ? (
                        <div className="w-4.5 h-4.5 bg-rose-500 border-2 border-white rounded-full flex items-center justify-center text-[7px] text-white font-extrabold" title="Account disabled">
                          ✕
                        </div>
                      ) : (
                        <div className="w-4.5 h-4.5 bg-amber-500 border-2 border-white rounded-full flex items-center justify-center text-[8px] text-white font-bold" title="Away / On leave">
                          ⏰
                        </div>
                      )}
                    </div>
                  </div>

                  {/* Right description info column */}
                  <div className="space-y-1 mt-1 flex-1">
                    <div className="flex flex-wrap items-center gap-1.5">
                      <h4 className="font-extrabold text-sm text-slate-800 truncate max-w-[200px]">
                        {emp.name}
                      </h4>
                      <span className={`text-[9px] font-bold px-1.5 py-0.5 rounded border uppercase shrink-0 ${getRoleBadgeColor(emp.role)}`}>
                        {emp.role}
                      </span>
                    </div>

                    <div className="space-y-1 pt-1 text-[11px] text-slate-550">
                      <div className="flex items-center gap-1.5">
                        <Mail className="w-3 h-3 text-slate-400" />
                        <span className="truncate">{emp.email}</span>
                      </div>
                      
                      <div className="flex items-center gap-1.5">
                        <Phone className="w-3 h-3 text-slate-400" />
                        <span>{emp.phone || "No phone configured"}</span>
                      </div>

                      {/* Password element (Very helpful for offline testing) */}
                      <div className="flex items-center gap-1.5 bg-slate-50 border border-slate-150 px-2 py-1 rounded mt-1.5 w-fit">
                        <Lock className="w-3 h-3 text-slate-500" />
                        <span className="font-mono text-[10px]">Pass:</span>
                        <span className="font-mono text-[10px] bg-slate-200/50 px-1 py-0.5 rounded text-amber-900 select-all font-semibold">
                          {isRevealed ? emp.password : "••••••••"}
                        </span>
                        <button
                          type="button"
                          onClick={() => togglePasswordReveal(emp.id)}
                          className="text-[#64748b] hover:text-slate-800 p-0.5"
                          title="Reveal Credentials"
                        >
                          {isRevealed ? <EyeOff className="w-3 h-3" /> : <Eye className="w-3 h-3" />}
                        </button>
                      </div>

                      <div className="text-[10px] text-slate-400 pt-1">
                        Joined Server: {emp.createdTime ? new Date(emp.createdTime).toLocaleDateString() : "Pending"}
                      </div>
                    </div>
                  </div>
                </div>

                {/* Status indicator and action row */}
                <div className="border-t border-slate-150 mt-4 pt-3 flex items-center justify-between">
                  <div>
                    {getStatusBadge(emp.status)}
                  </div>

                  <div className="flex items-center gap-1.5">
                    <button
                      onClick={() => handleToggleStatus(emp)}
                      disabled={isSelf || isFounder}
                      className={`text-[9.5px] font-bold px-2 py-1.5 rounded-lg border cursor-pointer transition-all ${
                        isSelf || isFounder
                          ? "opacity-30 cursor-not-allowed text-slate-400 border-slate-200"
                          : emp.status === "Active"
                          ? "bg-slate-50 text-rose-600 border-rose-100 hover:bg-rose-50"
                          : "bg-emerald-50 text-emerald-700 border-emerald-100 hover:bg-emerald-100"
                      }`}
                      title={emp.status === "Active" ? "Suspend coworker login keys" : "Unsuspend / Re-activate coworker access"}
                    >
                      {emp.status === "Active" ? "Revoke Keys" : "Grant Keys"}
                    </button>

                    <button
                      onClick={() => setEditingEmployee(emp)}
                      className="bg-slate-50 hover:bg-slate-100 border border-slate-200 text-slate-650 p-1.5 rounded-lg text-xs cursor-pointer"
                      title="Edit worker properties"
                    >
                      <Edit2 className="w-3.5 h-3.5 text-slate-550" />
                    </button>

                    <button
                      onClick={() => handleDeleteEmployee(emp.id, emp.name)}
                      disabled={isSelf || isFounder}
                      className={`p-1.5 rounded-lg border transition-all ${
                        isSelf || isFounder
                          ? "opacity-30 cursor-not-allowed border-slate-200 text-slate-350"
                          : "bg-slate-50 hover:bg-rose-50 border-slate-200 hover:border-rose-200 text-slate-500 hover:text-rose-600 cursor-pointer"
                      }`}
                      title="Permanently remove worker from central registry"
                    >
                      <Trash2 className="w-3.5 h-3.5" />
                    </button>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
