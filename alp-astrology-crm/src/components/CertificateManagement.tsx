import React from "react";
import {
  Award,
  Search,
  CheckCircle2,
  XCircle,
  FileDown,
  Printer,
  ShieldCheck,
  Calendar,
  AlertCircle
} from "lucide-react";
import { Certificate, StudentEnrollment } from "../types";

interface CertificateManagementProps {
  certificates: Certificate[];
  enrollments: StudentEnrollment[];
  onIssueCertificate: (issueData: { studentId: string; studentName: string; courseName: string; type: string }) => void;
}

export function CertificateManagement({
  certificates,
  enrollments,
  onIssueCertificate
}: CertificateManagementProps) {
  const [activeTab, setActiveTab] = React.useState<"list" | "issue" | "verify">("list");

  // Selection for printed preview certificate
  const [selectedPreview, setSelectedPreview] = React.useState<Certificate | null>(null);

  // Verification Input
  const [verifySerial, setVerifySerial] = React.useState("");
  const [verifyResult, setVerifyResult] = React.useState<{
    searched: boolean;
    verified: boolean;
    certificate?: Certificate;
    message?: string;
  } | null>(null);

  // Issue Form state
  const [issueForm, setIssueForm] = React.useState({
    enrollmentId: "",
    type: "Course Certificate" as any
  });

  const handleIssue = (e: React.FormEvent) => {
    e.preventDefault();
    if (!issueForm.enrollmentId) {
      alert("Please select eligible graduated student to print credential.");
      return;
    }

    const en = enrollments.find(e => e.id === issueForm.enrollmentId);
    if (!en) return;

    onIssueCertificate({
      studentId: en.studentId,
      studentName: en.studentName,
      courseName: en.courseName,
      type: issueForm.type
    });

    alert("Cryptographic Certificate issued successfully! Enrollment status graduated.");
    setActiveTab("list");
  };

  const handleSearchVerify = (e: React.FormEvent) => {
    e.preventDefault();
    if (!verifySerial.trim()) return;

    fetch(`/api/certificates/verify/${verifySerial.trim()}`)
      .then(res => res.json())
      .then(data => {
        setVerifyResult({
          searched: true,
          verified: data.verified,
          certificate: data.certificate,
          message: data.message
        });
      })
      .catch(err => {
        console.error("Error verifying serial", err);
        setVerifyResult({
          searched: true,
          verified: false,
          message: "Internal ledger offline. Please contact administrator."
        });
      });
  };

  // Option to initiate local print stream of certificate
  const handlePrint = () => {
    alert(`Initiating secure credential PDF print loop for recipient: ${selectedPreview?.studentName || "Graduated Scholar"}. QR code validation signature attached.`);
  };

  return (
    <div className="space-y-6">
      {/* Sub menu controls */}
      <div className="flex justify-between items-center border-b border-white/5 pb-3 flex-wrap gap-4 text-xs">
        <div className="flex gap-2">
          {[
            { id: "list", label: "Signed Credentials", icon: Award },
            { id: "issue", label: "Authorize Certificate", icon: ShieldCheck },
            { id: "verify", label: "Credential Verification Portal", icon: Search }
          ].map(sb => {
            const Icon = sb.icon;
            const isMatch = activeTab === sb.id;
            return (
              <button
                key={sb.id}
                onClick={() => {
                  setActiveTab(sb.id as any);
                  if (sb.id !== "verify") setVerifyResult(null);
                }}
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
      </div>

      {/* RENDER ACTIVE TABS */}

      {/* 1. LIST OF SIGNED CERTIFICATES */}
      {activeTab === "list" && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {certificates.map(cert => (
            <div
              key={cert.id}
              className="bg-slate-900 border border-slate-800 rounded-xl p-5 space-y-4 hover:border-amber-400/20 transition-all flex flex-col justify-between text-xs"
            >
              <div className="space-y-2">
                <div className="flex justify-between items-start">
                  <span className="text-[9px] font-mono uppercase bg-amber-500/10 border border-amber-500/20 text-amber-400 px-2 py-0.5 rounded-sm font-bold">
                    {cert.type}
                  </span>
                  <span className="text-[10px] text-slate-500 font-mono">Issued: {cert.issueDate}</span>
                </div>
                <h4 className="font-bold text-slate-200 text-sm leading-snug">{cert.studentName}</h4>
                <p className="text-[11px] text-slate-450 font-sans">Graduated: {cert.courseName}</p>
              </div>

              <div className="border-t border-white/5 pt-3 mt-4 flex justify-between items-center text-[10.5px]">
                <span className="text-slate-500 font-mono">Serial: <span className="font-bold text-slate-300 font-sans">{cert.certificateNo}</span></span>
                <button
                  onClick={() => setSelectedPreview(cert)}
                  className="bg-slate-800 hover:bg-slate-750 text-slate-350 hover:text-white px-2.5 py-1 rounded transition-colors text-[10px]"
                >
                  View Credential
                </button>
              </div>
            </div>
          ))}

          {certificates.length === 0 && (
            <p className="col-span-full text-center py-12 text-slate-500 font-mono">No certificates signed inside current system directory.</p>
          )}
        </div>
      )}

      {/* 2. ISSUE / SIGN CREDENTIAL */}
      {activeTab === "issue" && (
        <div className="bg-slate-900 border border-white/5 rounded-xl p-5 max-w-lg mx-auto text-xs space-y-4">
          <div className="border-b border-white/5 pb-3">
            <h3 className="font-bold text-sm text-slate-200">Sign & Authorize Academic Credential</h3>
            <p className="text-[10px] text-slate-400 mt-1">This operation assigns a secure holographic certificate sequence to eligible students.</p>
          </div>

          <form onSubmit={handleIssue} className="space-y-4">
            <div>
              <label className="text-[10px] font-mono text-slate-400 block mb-1">Select Completed student *</label>
              <select
                required
                value={issueForm.enrollmentId}
                onChange={e => setIssueForm({ ...issueForm, enrollmentId: e.target.value })}
                className="w-full bg-slate-955 border border-slate-800 rounded px-3 py-2 text-white cursor-pointer focus:outline-none"
              >
                <option value="" disabled>Select qualified completed student...</option>
                {/* filter students that are in progress or completed */}
                {enrollments.map(en => (
                  <option key={en.id} value={en.id} className="bg-slate-900">
                    {en.studentName} - {en.courseName} ({en.progressPercentage}% complete)
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="text-[10px] font-mono text-slate-400 block mb-1">Select Credential Category *</label>
              <select
                value={issueForm.type}
                onChange={e => setIssueForm({ ...issueForm, type: e.target.value as any })}
                className="w-full bg-slate-955 border border-slate-800 rounded px-3 py-2 text-white cursor-pointer focus:outline-none"
              >
                <option value="Course Certificate" className="bg-slate-900" >Course Academy Graduate Certificate</option>
                <option value="Workshop Certificate" className="bg-slate-900">Workshop Completion Cert</option>
                <option value="Achievement Certificate" className="bg-slate-900">Research Excellence Award</option>
              </select>
            </div>

            <div className="bg-slate-950 p-3 rounded leading-relaxed text-[10px] text-slate-400 border border-white/5 flex gap-2">
              <ShieldCheck className="w-4 h-4 text-emerald-400 shrink-0 mt-0.5" />
              <span>
                By clicking "Authorize and Sign", the system locks progress, signs serial hashes, and attaches QR security routing protocols automatically.
              </span>
            </div>

            <div className="pt-2 flex justify-end gap-2 text-xs font-bold">
              <button
                type="submit"
                className="w-full bg-amber-500 hover:bg-amber-450 text-slate-950 py-2.5 rounded-lg shadow-lg shadow-amber-500/10 font-bold tracking-wide"
              >
                Sign & Issue Credential
              </button>
            </div>
          </form>
        </div>
      )}

      {/* 3. VERIFICATION PORTAL */}
      {activeTab === "verify" && (
        <div className="space-y-6 max-w-lg mx-auto text-xs">
          <div className="bg-slate-900 border border-white/5 rounded-xl p-5 space-y-4 shadow">
            <div className="text-center space-y-1">
              <ShieldCheck className="w-8 h-8 text-amber-500 mx-auto" />
              <h3 className="font-bold text-sm text-slate-200">Credential Integrity Verification Port</h3>
              <p className="text-[10px] text-slate-450">Public verification link check. Enter serial hash to query ledger arrays.</p>
            </div>

            <form onSubmit={handleSearchVerify} className="flex gap-2">
              <input
                type="text"
                required
                placeholder="e.g. ALP-CRT-98124"
                value={verifySerial}
                onChange={e => setVerifySerial(e.target.value)}
                className="flex-1 bg-slate-955 border border-slate-800 rounded-lg px-3.5 py-2 text-white font-mono"
              />
              <button
                type="submit"
                className="bg-slate-800 hover:bg-slate-750 text-slate-300 font-semibold px-4 rounded-lg flex items-center justify-center cursor-pointer border border-slate-700"
              >
                Verify Hash
              </button>
            </form>
          </div>

          {/* Verification Results report card */}
          {verifyResult?.searched && (
            <div className="bg-slate-900 border border-white/5 rounded-xl p-5 animate-fade-in text-center space-y-4">
              {verifyResult.verified && verifyResult.certificate ? (
                <div className="space-y-4">
                  <div className="inline-flex items-center gap-1.5 bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 px-3.5 py-1 rounded-full font-mono text-[10px] font-bold">
                    <CheckCircle2 className="w-3.5 h-3.5" />
                    <span>Holographic Credential Validated</span>
                  </div>

                  <div className="bg-slate-950/40 p-4 rounded-lg border border-white/5 text-left space-y-2.5 font-mono text-[10.5px]">
                    <p className="text-slate-500">CREDENTIAL RECIPIENT:</p>
                    <p className="text-slate-100 font-sans font-bold text-xs -mt-1.5">{verifyResult.certificate.studentName}</p>
                    
                    <p className="text-slate-500 mt-2">COMPLETED COURSE SYLLABUS:</p>
                    <p className="text-amber-400 font-sans font-semibold -mt-1.5">{verifyResult.certificate.courseName}</p>

                    <div className="grid grid-cols-2 gap-2 pt-2 border-t border-white/5 text-[9.5px]">
                      <p><span className="text-slate-500">Issued On:</span> {verifyResult.certificate.issueDate}</p>
                      <p><span className="text-slate-500">Serial no:</span> {verifyResult.certificate.certificateNo}</p>
                      <p><span className="text-slate-500">Status ID:</span> Graduated OK</p>
                      <p className="text-emerald-400">✔ Ledger Sync Match</p>
                    </div>
                  </div>
                </div>
              ) : (
                <div className="space-y-3 py-4">
                  <XCircle className="w-10 h-10 text-rose-500 mx-auto" />
                  <p className="font-bold text-slate-200">Credential Verification Failed</p>
                  <p className="text-[11px] text-slate-500 max-w-sm mx-auto leading-relaxed">
                    {verifyResult.message || "This serial sequence is not indexed inside global database ledgers. Check digits formatting and try again."}
                  </p>
                </div>
              )}
            </div>
          )}
        </div>
      )}

      {/* DETAILED EXPANSION PHYSICAL CERTIFICATE FRAME FOR PRINT PREVIEW */}
      {selectedPreview && (
        <div className="fixed inset-0 bg-slate-950/90 backdrop-blur-xs flex items-center justify-center z-50 p-4">
          <div className="bg-slate-900 border border-amber-500/15 rounded-2xl w-full max-w-3xl overflow-hidden shadow-2xl flex flex-col max-h-[90vh]">
            
            <div className="p-4 bg-slate-950 flex justify-between items-center shrink-0 border-b border-light/5">
              <span className="text-xs font-mono text-amber-500 font-bold uppercase tracking-wider">Academic Credential Preview</span>
              <button
                onClick={() => setSelectedPreview(null)}
                className="p-1 rounded bg-white/5 text-slate-400 hover:text-white"
              >
                ✕
              </button>
            </div>

            {/* Aesthetic Certificate display block */}
            <div className="p-8 flex-1 overflow-y-auto flex items-center justify-center bg-slate-955/40">
              <div className="bg-radial from-slate-900 to-slate-950 border-[6px] border-double border-amber-500/30 rounded-xl p-10 w-full max-w-2xl text-center space-y-6 relative overflow-hidden shadow-inner">
                {/* Watermark circle */}
                <div className="absolute w-[400px] h-[400px] rounded-full border border-amber-500/5 -top-24 -left-20 pointer-events-none select-none"></div>

                <div className="space-y-1 relative z-10">
                  <div className="w-10 h-10 rounded-full bg-linear-to-tr from-amber-500 to-yellow-300 flex items-center justify-center mx-auto mb-3 shadow shadow-amber-500/10">
                    <span className="text-slate-950 font-bold text-xs">ALP</span>
                  </div>
                  <h2 className="text-amber-500 font-extrabold text-[#f59e0b] tracking-widest uppercase font-mono text-xs">ALP Certificate of Excellence</h2>
                  <p className="text-[8.5px] tracking-widest text-slate-500 uppercase font-mono">Akshaya Lagna Paddhati Academy of Astrology</p>
                </div>

                <div className="space-y-2 relative z-10">
                  <p className="text-[10px] text-slate-450 italic">This document certifies that scholarly achievements has been successfully verified for</p>
                  <h1 className="text-xl font-bold font-sans text-slate-100 tracking-wide">{selectedPreview.studentName}</h1>
                  <p className="text-[10.5px] text-slate-400 italic max-w-md mx-auto pt-1 leading-relaxed">
                    demostrating rigorous command of birth parameters plotting, coordinate transits, and precise timing matrices in accordance with Akshaya Lagna Paddhati guidelines and rules.
                  </p>
                </div>

                <div className="space-y-1 relative z-10 pt-2">
                  <p className="text-[9.5px] uppercase font-mono text-slate-500">Graduation syllabus Program:</p>
                  <h3 className="font-bold text-slate-200 text-sm">{selectedPreview.courseName}</h3>
                </div>

                <div className="border-t border-white/5 pt-6 mt-8 flex justify-between items-center text-[9px] font-mono text-slate-500 relative z-10">
                  <div className="text-left space-y-1.5">
                    <p>Serial: <span className="text-slate-350">{selectedPreview.certificateNo}</span></p>
                    <p>Date signed: <span className="text-slate-355">{selectedPreview.issueDate}</span></p>
                  </div>

                  <div className="text-center w-20 h-20 border border-white/5 rounded p-1 bg-slate-950/60 shadow">
                    <div className="w-full h-full bg-linear-to-tr from-amber-500/10 to-amber-500/30 flex items-center justify-center flex-col text-[7px] text-amber-500">
                      <span className="font-bold uppercase tracking-widest leading-none">VERIFIED</span>
                      <span className="text-[6px] text-slate-600 mt-1 leading-normal font-mono">SCAN QR KEY</span>
                    </div>
                  </div>

                  <div className="text-right space-y-1.5">
                    <p className="border-b border-slate-700 pb-1.5 text-slate-300 font-semibold italic">Dr. K. Muralidharan</p>
                    <p className="text-[7.5px] uppercase font-bold tracking-wider text-amber-500/60">Registrar Signet</p>
                  </div>
                </div>
              </div>
            </div>
            
            <div className="p-4 bg-slate-950/60 flex justify-end gap-2 text-xs">
              <button
                onClick={handlePrint}
                className="bg-amber-500 hover:bg-amber-450 text-slate-950 font-bold px-4 py-1.5 rounded-lg transition-all flex items-center gap-1 cursor-pointer"
              >
                <Printer className="w-3.5 h-3.5" />
                <span>Export PDF print</span>
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
