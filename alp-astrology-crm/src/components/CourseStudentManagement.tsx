import React from "react";
import {
  GraduationCap,
  Plus,
  BookOpen,
  Calendar,
  Layers,
  Users,
  Search,
  CheckCircle,
  XCircle,
  Percent,
  TrendingUp,
  MapPin,
  ClipboardList
} from "lucide-react";
import { Course, Batch, Student, StudentEnrollment } from "../types";

interface CourseStudentManagementProps {
  courses: Course[];
  batches: Batch[];
  students: Student[];
  enrollments: StudentEnrollment[];
  onAddStudent: (studData: Partial<Student>) => void;
  onEnrollStudent: (enrollData: { studentId: string; courseId: string; batchId: string }) => void;
  onUpdateEnrollment: (enrId: string, fields: Partial<StudentEnrollment>) => void;
}

export function CourseStudentManagement({
  courses,
  batches,
  students,
  enrollments,
  onAddStudent,
  onEnrollStudent,
  onUpdateEnrollment
}: CourseStudentManagementProps) {
  // Navigation subtabs: "curriculum", "students" or "attendance"
  const [panelMode, setPanelMode] = React.useState<"curriculum" | "students" | "attendance">("students");

  // Filter queries
  const [studentSearch, setStudentSearch] = React.useState("");
  const [selectedBatchFilter, setSelectedBatchFilter] = React.useState("All");

  // Add student Form UI toggle
  const [isOpenAddStudent, setIsOpenAddStudent] = React.useState(false);
  const [newStudent, setNewStudent] = React.useState({
    name: "",
    phone: "",
    email: "",
    country: "India",
    occupation: "Student"
  });

  // Enroll existing student Form UI toggle
  const [isOpenEnroll, setIsOpenEnroll] = React.useState(false);
  const [enrollForm, setEnrollForm] = React.useState({
    studentId: "",
    courseId: courses[0]?.id || "",
    batchId: batches[0]?.id || ""
  });

  // Attendance batch sheet selector
  const [activeAttendanceBatch, setActiveAttendanceBatch] = React.useState<string>(batches[0]?.id || "");

  const filteredStudents = students.filter(s => {
    const keyword = studentSearch.toLowerCase();
    return s.name.toLowerCase().includes(keyword) || s.phone.includes(keyword) || s.email.toLowerCase().includes(keyword) || s.country.toLowerCase().includes(keyword);
  });

  const handleRegisterStudent = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newStudent.name || !newStudent.phone || !newStudent.email) {
      alert("Please fill name, email and phone to index student profile.");
      return;
    }
    onAddStudent(newStudent);
    setIsOpenAddStudent(false);
    setNewStudent({ name: "", phone: "", email: "", country: "India", occupation: "Student" });
  };

  const handleEnrollExisting = (e: React.FormEvent) => {
    e.preventDefault();
    if (!enrollForm.studentId || !enrollForm.courseId || !enrollForm.batchId) {
      alert("Select student, course syllabus, and batch cohort target.");
      return;
    }
    onEnrollStudent(enrollForm);
    setIsOpenEnroll(false);
    alert("Student mapped to academy cohort successfully! Unpaid fees invoice drafted.");
  };

  const handleToggleAttendance = (enrId: string, currentAttendance: number) => {
    // Simulates checking student on roll sheet
    // If we toggle, let's fluctuate attendance percentage
    const step = Math.random() > 0.5 ? 5 : -5;
    const nextVal = Math.min(100, Math.max(0, currentAttendance + step));
    onUpdateEnrollment(enrId, { attendancePercentage: nextVal });
  };

  const handleProgressChange = (enrId: string, val: number) => {
    onUpdateEnrollment(enrId, { progressPercentage: Math.min(100, Math.max(0, val)) });
  };

  return (
    <div className="space-y-6">
      {/* Sub menu tabs controllers */}
      <div className="flex border-b border-white/5 pb-3 justify-between items-center flex-wrap gap-4 text-xs">
        <div className="flex gap-2">
          {[
            { id: "students", label: "Students Registry", icon: Users },
            { id: "curriculum", label: "Syllabus Tiers", icon: BookOpen },
            { id: "attendance", label: "Attendance sheets", icon: ClipboardList }
          ].map(sb => {
            const Icon = sb.icon;
            const isMatch = panelMode === sb.id;
            return (
              <button
                key={sb.id}
                onClick={() => setPanelMode(sb.id as any)}
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

        {panelMode === "students" && (
          <div className="flex gap-2.5">
            <button
              onClick={() => setIsOpenAddStudent(true)}
              className="border border-white/10 text-slate-300 hover:bg-slate-800 px-3.5 py-1.5 rounded-lg font-semibold cursor-pointer"
            >
              Onboard Student
            </button>
            <button
              onClick={() => setIsOpenEnroll(true)}
              className="bg-amber-500 hover:bg-amber-450 text-slate-950 px-4 py-1.5 rounded-lg font-bold shadow-lg shadow-amber-500/5 cursor-pointer"
            >
              Enroll Program Batch
            </button>
          </div>
        )}
      </div>

      {/* RENDER ACTIVE SUBMODES */}

      {/* 1. CURRICULUM SYLLABUS TIERS */}
      {panelMode === "curriculum" && (
        <div className="space-y-6">
          {/* Courses block */}
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
            {courses.map(crs => (
              <div key={crs.id} className="bg-slate-900 border border-slate-800 rounded-xl p-4.5 space-y-3 shadow flex flex-col justify-between text-xs">
                <div className="space-y-1.5">
                  <span className="text-[10px] font-mono tracking-wider font-bold text-amber-500">{crs.code}</span>
                  <h4 className="font-semibold text-slate-200 text-sm leading-tight">{crs.name}</h4>
                  <p className="text-slate-400 leading-relaxed text-[11px] font-sans pt-1">{crs.description}</p>
                </div>
                <div className="border-t border-white/5 pt-3 mt-4 flex justify-between items-center text-[10.5px]">
                  <span className="text-slate-500">Duration: {crs.durationWeeks} Weeks</span>
                  <span className="text-amber-400 font-bold bg-amber-500/5 px-2.5 py-0.5 rounded border border-amber-500/10">
                    INR {crs.fees.toLocaleString()}
                  </span>
                </div>
              </div>
            ))}
          </div>

          {/* Batches scheduler summary */}
          <div className="bg-slate-900/60 border border-white/5 rounded-xl p-5">
            <h3 className="font-bold text-sm text-slate-200 mb-4 flex items-center gap-1.5">
              <Calendar className="w-4 h-4 text-amber-500" />
              <span>Academic Cohort Batches (Active/Upcoming)</span>
            </h3>

            <div className="space-y-2 max-h-72 overflow-y-auto pr-1">
              {batches.map(bat => (
                <div
                  key={bat.id}
                  className="flex justify-between items-center p-3.5 bg-slate-950/40 border border-white/5 hover:border-slate-800 rounded-lg text-xs"
                >
                  <div className="space-y-1 pr-4">
                    <p className="font-bold text-slate-200">{bat.name}</p>
                    <p className="text-slate-400 text-[10.5px] font-sans">Syllabus: {bat.courseName}</p>
                  </div>
                  <div className="text-[10.5px] font-mono text-slate-400 text-right space-y-0.5">
                    <p>Starts: {bat.startDate}</p>
                    <p>Schedule: {bat.schedule}</p>
                    <p className="text-[9px] text-amber-400 uppercase font-semibold">Trainer: {bat.trainerName}</p>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* 2. STUDENTS REGISTRY */}
      {panelMode === "students" && (
        <div className="space-y-4">
          {/* Filters search */}
          <div className="flex gap-3 max-w-sm">
            <input
              type="text"
              placeholder="Search students registry..."
              value={studentSearch}
              onChange={e => setStudentSearch(e.target.value)}
              className="w-full bg-slate-900 border border-slate-800 text-xs rounded-lg px-3.5 py-2 text-white"
            />
          </div>

          <div className="bg-slate-900/60 border border-white/5 rounded-xl overflow-hidden shadow">
            {/* Header column lists */}
            <div className="bg-slate-950/30 border-b border-light/5 px-4 py-2 text-[10px] font-mono text-slate-500 uppercase tracking-widest grid grid-cols-12 gap-2 font-bold select-none text-left">
              <span className="col-span-3">Student Name</span>
              <span className="col-span-2">Contact</span>
              <span className="col-span-4">Active Course Academy</span>
              <span className="col-span-1 text-center">Lessons</span>
              <span className="col-span-1 text-center">Class Roll</span>
              <span className="col-span-1 text-right">Status</span>
            </div>

            {/* list frame */}
            <div className="divide-y divide-white/5">
              {filteredStudents.map(stud => {
                const studEnroll = enrollments.filter(e => e.studentId === stud.id);
                return (
                  <div key={stud.id} className="px-4 py-3.5 grid grid-cols-12 gap-2 text-xs items-center text-left hover:bg-slate-950/10">
                    <div className="col-span-3 font-semibold text-slate-100 flex items-center gap-2">
                      <div className="w-8 h-8 rounded-full bg-slate-850 flex items-center justify-center border border-white/5 text-[10px] text-amber-400 capitalize font-mono shrink-0 font-bold">
                        {stud.name.substring(0, 2)}
                      </div>
                      <div className="min-w-0">
                        <p className="truncate">{stud.name}</p>
                        <p className="text-[10px] text-slate-400 font-mono italic">{stud.country}</p>
                      </div>
                    </div>

                    <div className="col-span-2 text-[11px] text-slate-400 font-mono space-y-0.5 min-w-0 truncate">
                      <p className="truncate">{stud.phone}</p>
                      <p className="truncate">{stud.email}</p>
                    </div>

                    <div className="col-span-4 min-w-0">
                      {studEnroll.length > 0 ? (
                        <div className="space-y-1 pr-4">
                          {studEnroll.map(se => (
                            <div key={se.id} className="text-[10.5px]">
                              <p className="font-semibold text-slate-355 truncate">{se.courseName}</p>
                              <p className="text-[9.5px] text-slate-510 font-sans truncate">Batch: {se.batchName}</p>
                            </div>
                          ))}
                        </div>
                      ) : (
                        <p className="text-[10px] text-slate-500 italic">Not enrolled in any active batch</p>
                      )}
                    </div>

                    <div className="col-span-1 text-center font-mono">
                      {studEnroll.length > 0 ? (
                        <div className="space-y-1">
                          {studEnroll.map(se => (
                            <div key={se.id} className="flex flex-col items-center">
                              <span className="font-bold text-[10.5px] text-slate-200">{se.progressPercentage}%</span>
                              <input
                                type="range"
                                min="0"
                                max="100"
                                value={se.progressPercentage}
                                onChange={e => handleProgressChange(se.id, Number(e.target.value))}
                                className="w-12 h-1 accent-amber-500 cursor-ew-resize bg-slate-800"
                              />
                            </div>
                          ))}
                        </div>
                      ) : (
                        "-"
                      )}
                    </div>

                    <div className="col-span-1 text-center font-mono">
                      {studEnroll.length > 0 ? (
                        <div className="space-y-1 text-[10.5px]">
                          {studEnroll.map(se => (
                            <p
                              key={se.id}
                              onClick={() => handleToggleAttendance(se.id, se.attendancePercentage)}
                              className="font-bold text-slate-300 hover:text-amber-400 cursor-pointer underline decoration-dotted"
                              title="Click to toggle/fluctuate attendance roll check"
                            >
                              {se.attendancePercentage}%
                            </p>
                          ))}
                        </div>
                      ) : (
                        "-"
                      )}
                    </div>

                    <div className="col-span-1 text-right font-mono">
                      {studEnroll.map(se => (
                        <span
                          key={se.id}
                          className={`text-[9px] px-1.5 py-0.5 rounded font-bold uppercase ${
                            se.status === "In Progress"
                              ? "bg-blue-500/10 text-blue-400"
                              : se.status === "Completed"
                              ? "bg-emerald-500/10 text-emerald-400"
                              : "bg-slate-800 text-slate-400"
                          }`}
                        >
                          {se.status}
                        </span>
                      ))}
                    </div>
                  </div>
                );
              })}

              {filteredStudents.length === 0 && (
                <p className="text-center py-12 text-slate-500 italic text-[11px]">No students matched query lists.</p>
              )}
            </div>
          </div>
        </div>
      )}

      {/* 3. ATTENDANCE SHEETS ROLL CALL */}
      {panelMode === "attendance" && (
        <div className="bg-slate-900 border border-white/5 rounded-xl p-5 space-y-4">
          <div className="flex justify-between items-center border-b border-light/5 pb-3">
            <div>
              <h3 className="font-bold text-sm text-slate-200 flex items-center gap-1.5">
                <ClipboardList className="w-4 h-4 text-emerald-400" />
                <span>Classroom roll sheet checklist planner</span>
              </h3>
              <p className="text-[10px] text-slate-400 mt-0.5">Time sheets checked here automatically recalculate attendance parameters.</p>
            </div>

            <div>
              <select
                value={activeAttendanceBatch}
                onChange={e => setActiveAttendanceBatch(e.target.value)}
                className="bg-slate-950 border border-slate-800 text-slate-300 px-3 py-1.5 rounded-lg text-xs focus:outline-none cursor-pointer font-semibold"
              >
                {batches.map(b => (
                  <option key={b.id} value={b.id} className="bg-slate-900">{b.name}</option>
                ))}
              </select>
            </div>
          </div>

          <div className="space-y-2">
            {enrollments
              .filter(e => e.batchId === activeAttendanceBatch)
              .map(en => (
                <div key={en.id} className="flex justify-between items-center p-3.5 bg-slate-955 border border-white/5 rounded-lg text-xs">
                  <div>
                    <p className="font-bold text-slate-100">{en.studentName}</p>
                    <p className="text-[10px] text-slate-400 font-mono mt-0.5">Course enrollment progress: {en.progressPercentage}%</p>
                  </div>

                  <div className="flex items-center gap-5">
                    <div className="text-right text-[10.5px]">
                      <span className="text-slate-500 mr-2">Calculated Attendance:</span>
                      <span className="font-mono font-bold text-slate-200 bg-slate-900 px-2 py-0.5 rounded border border-white/5">
                        {en.attendancePercentage}%
                      </span>
                    </div>

                    <div className="flex gap-1.5">
                      <button
                        onClick={() => onUpdateEnrollment(en.id, { attendancePercentage: Math.min(100, en.attendancePercentage + 5) })}
                        className="bg-emerald-500/10 hover:bg-emerald-500 text-emerald-400 hover:text-slate-950 border border-emerald-500/10 px-2.5 py-1.5 rounded text-[10px] font-bold transition-all cursor-pointer"
                      >
                        ✔ Present
                      </button>
                      <button
                        onClick={() => onUpdateEnrollment(en.id, { attendancePercentage: Math.max(0, en.attendancePercentage - 5) })}
                        className="bg-rose-500/10 hover:bg-rose-500 text-rose-400 hover:text-slate-950 border border-rose-500/10 px-2.5 py-1.5 rounded text-[10px] font-bold transition-all cursor-pointer"
                      >
                        ✕ Abs Present
                      </button>
                    </div>
                  </div>
                </div>
              ))}

            {enrollments.filter(e => e.batchId === activeAttendanceBatch).length === 0 && (
              <p className="text-center py-8 text-slate-500 italic font-mono">No students enrolled inside this batch cohort registers.</p>
            )}
          </div>
        </div>
      )}

      {/* NEW STUDENT MANUALLY CREATE MODAL */}
      {isOpenAddStudent && (
        <div className="fixed inset-0 bg-slate-950/80 backdrop-blur-xs flex items-center justify-center z-50 p-4">
          <div className="bg-slate-900 border border-amber-500/15 rounded-xl w-full max-w-md overflow-hidden text-xs">
            <div className="p-4 bg-slate-950/65 border-b border-white/5 flex justify-between items-center">
              <h3 className="font-bold text-slate-100 text-sm">Onboard Academy Student</h3>
              <button onClick={() => setIsOpenAddStudent(false)} className="text-slate-400 hover:text-white p-1 rounded">✕</button>
            </div>

            <form onSubmit={handleRegisterStudent} className="p-5 space-y-4">
              <div>
                <label className="text-[10px] font-mono text-slate-400 block mb-1">Student Legal Name *</label>
                <input
                  type="text"
                  required
                  placeholder="Kalyan Kumar"
                  value={newStudent.name}
                  onChange={e => setNewStudent({ ...newStudent, name: e.target.value })}
                  className="w-full bg-slate-955 border border-slate-800 rounded px-2.5 py-1.5 text-white"
                />
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="text-[10px] font-mono text-slate-400 block mb-1">Mobile Contact *</label>
                  <input
                    type="text"
                    required
                    placeholder="+91 94441 55667"
                    value={newStudent.phone}
                    onChange={e => setNewStudent({ ...newStudent, phone: e.target.value })}
                    className="w-full bg-slate-955 border border-slate-800 rounded px-2.5 py-1.5 text-white"
                  />
                </div>
                <div>
                  <label className="text-[10px] font-mono text-slate-400 block mb-1">Academics Email *</label>
                  <input
                    type="email"
                    required
                    placeholder="kalyan@outlook.com"
                    value={newStudent.email}
                    onChange={e => setNewStudent({ ...newStudent, email: e.target.value })}
                    className="w-full bg-slate-955 border border-slate-800 rounded px-2.5 py-1.5 text-white"
                  />
                </div>
                <div>
                  <label className="text-[10px] font-mono text-slate-400 block mb-1">Country</label>
                  <input
                    type="text"
                    value={newStudent.country}
                    onChange={e => setNewStudent({ ...newStudent, country: e.target.value })}
                    className="w-full bg-slate-955 border border-slate-800 rounded px-2.5 py-1.5 text-white"
                  />
                </div>
                <div>
                  <label className="text-[10px] font-mono text-slate-400 block mb-1">Occupation</label>
                  <input
                    type="text"
                    value={newStudent.occupation}
                    onChange={e => setNewStudent({ ...newStudent, occupation: e.target.value })}
                    className="w-full bg-slate-955 border border-slate-800 rounded px-2.5 py-1.5 text-white"
                  />
                </div>
              </div>

              <div className="pt-2 flex justify-end gap-2 text-xs font-bold">
                <button
                  type="button"
                  onClick={() => setIsOpenAddStudent(false)}
                  className="bg-slate-800 hover:bg-slate-755 text-slate-350 border border-slate-705 px-4 py-2 rounded-lg cursor-pointer"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="bg-amber-500 hover:bg-amber-450 text-slate-950 px-4 py-2 rounded-lg shadow-lg shadow-amber-500/10 cursor-pointer"
                >
                  Onboard Student Profile
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ENROLL PROGRAM EXISTING MODAL */}
      {isOpenEnroll && (
        <div className="fixed inset-0 bg-slate-950/80 backdrop-blur-xs flex items-center justify-center z-50 p-4">
          <div className="bg-slate-900 border border-amber-500/15 rounded-xl w-full max-w-sm overflow-hidden text-xs">
            <div className="p-4 bg-slate-950/65 border-b border-white/5 flex justify-between items-center">
              <h3 className="font-bold text-slate-100 text-sm">Enroll Existing Student into Batch</h3>
              <button onClick={() => setIsOpenEnroll(false)} className="text-slate-400 hover:text-white p-1 rounded">✕</button>
            </div>

            <form onSubmit={handleEnrollExisting} className="p-5 space-y-4">
              <div>
                <label className="text-[10px] font-mono text-slate-400 block mb-1">Select Student profile *</label>
                <select
                  required
                  value={enrollForm.studentId}
                  onChange={e => setEnrollForm({ ...enrollForm, studentId: e.target.value })}
                  className="w-full bg-slate-955 border border-slate-800 rounded px-2.5 py-1.5 text-white cursor-pointer"
                >
                  <option value="" disabled>Select student...</option>
                  {students.map(s => (
                    <option key={s.id} value={s.id} className="bg-slate-900">{s.name}</option>
                  ))}
                </select>
              </div>

              <div>
                <label className="text-[10px] font-mono text-slate-400 block mb-1">Select Syllabus Course tier *</label>
                <select
                  required
                  value={enrollForm.courseId}
                  onChange={e => setEnrollForm({ ...enrollForm, courseId: e.target.value })}
                  className="w-full bg-slate-955 border border-slate-800 rounded px-2.5 py-1.5 text-white cursor-pointer"
                >
                  {courses.map(s => (
                    <option key={s.id} value={s.id} className="bg-slate-900">{s.name}</option>
                  ))}
                </select>
              </div>

              <div>
                <label className="text-[10px] font-mono text-slate-400 block mb-1">Select Cohort Batch Assignment *</label>
                <select
                  required
                  value={enrollForm.batchId}
                  onChange={e => setEnrollForm({ ...enrollForm, batchId: e.target.value })}
                  className="w-full bg-slate-955 border border-slate-800 rounded px-2.5 py-1.5 text-white cursor-pointer"
                >
                  {batches.map(s => (
                    <option key={s.id} value={s.id} className="bg-slate-900">{s.name}</option>
                  ))}
                </select>
              </div>

              <div className="pt-2 flex justify-end gap-2 text-xs font-bold">
                <button
                  type="button"
                  onClick={() => setIsOpenEnroll(false)}
                  className="bg-slate-800 hover:bg-slate-755 text-slate-350 border border-slate-705 px-4 py-2 rounded-lg cursor-pointer"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="bg-amber-500 hover:bg-amber-450 text-slate-955 px-4 py-2 rounded-lg shadow-lg shadow-amber-500/10 cursor-pointer"
                >
                  Enroll and Bill
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
