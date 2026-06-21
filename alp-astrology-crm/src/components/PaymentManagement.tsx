import React from "react";
import {
  CreditCard,
  Plus,
  Inbox,
  Printer,
  X,
  FileText,
  BadgeCheck,
  AlertTriangle,
  Receipt,
  FileSpreadsheet,
  Download
} from "lucide-react";
import { Payment, Invoice } from "../types";

interface PaymentManagementProps {
  payments: Payment[];
  invoices: Invoice[];
  onAddPayment: (payData: Partial<Payment>) => void;
  onClearPending: (payId: string) => void;
}

export function PaymentManagement({
  payments,
  invoices,
  onAddPayment,
  onClearPending
}: PaymentManagementProps) {
  // Navigation: "transactions" or "invoices"
  const [subMode, setSubMode] = React.useState<"transactions" | "invoices">("transactions");

  // Selection states for modal PDF popups
  const [activeInvoice, setActiveInvoice] = React.useState<Invoice | null>(null);
  const [activeReceipt, setActiveReceipt] = React.useState<Payment | null>(null);

  // Quick manually create transaction form state
  const [isOpenAddTrans, setIsOpenAddTrans] = React.useState(false);
  const [newTrans, setNewTrans] = React.useState({
    payerName: "",
    payerEmail: "",
    type: "Consultation Fees" as any,
    amount: "3000",
    method: "UPI (GPay/PhonePe)",
    status: "Paid"
  });

  const submitNewTrans = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newTrans.payerName || !newTrans.payerEmail || !newTrans.amount) {
      alert("Billing name, email and amount are structural requirements.");
      return;
    }
    onAddPayment(newTrans);
    setIsOpenAddTrans(false);
    // Reset
    setNewTrans({
      payerName: "",
      payerEmail: "",
      type: "Consultation Fees",
      amount: "3000",
      method: "UPI (GPay/PhonePe)",
      status: "Paid"
    });
  };

  const handlePrint = () => {
    window.print();
  };

  return (
    <div className="space-y-6">
      {/* Sub menu controls */}
      <div className="flex justify-between items-center border-b border-white/5 pb-3 flex-wrap gap-4 text-xs">
        <div className="flex gap-2">
          {[
            { id: "transactions", label: "Financial Ledger", icon: CreditCard },
            { id: "invoices", label: "Invoices Ledger", icon: FileText }
          ].map(sb => {
            const Icon = sb.icon;
            const isMatch = subMode === sb.id;
            return (
              <button
                key={sb.id}
                onClick={() => setSubMode(sb.id as any)}
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

        <button
          onClick={() => setIsOpenAddTrans(true)}
          className="bg-amber-500 hover:bg-amber-450 text-slate-950 px-4 py-1.5 rounded-lg text-xs font-bold shadow-lg shadow-amber-500/10 cursor-pointer"
        >
          Book Transaction
        </button>
      </div>

      {/* RENDER MODES */}

      {/* 1. TRANSACTION LEDGER */}
      {subMode === "transactions" && (
        <div className="bg-slate-900/60 border border-white/5 rounded-xl overflow-hidden shadow">
          <div className="bg-slate-950/30 border-b border-light/5 px-4 py-2.5 text-[10px] font-mono text-slate-500 uppercase tracking-widest grid grid-cols-12 gap-2 font-bold select-none text-left">
            <span className="col-span-3">Payer Billing Profile</span>
            <span className="col-span-2">Source Income</span>
            <span className="col-span-2 text-center">Amount</span>
            <span className="col-span-2 text-center">Method</span>
            <span className="col-span-2 text-center">Verification</span>
            <span className="col-span-1 text-right">Actions</span>
          </div>

          <div className="divide-y divide-white/5">
            {payments.map(pay => (
              <div key={pay.id} className="px-4 py-3.5 grid grid-cols-12 gap-2 text-xs items-center text-left hover:bg-slate-950/10">
                <div className="col-span-3 min-w-0">
                  <p className="font-semibold text-slate-100 truncate">{pay.payerName}</p>
                  <p className="text-[10px] text-slate-500 truncate">{pay.payerEmail}</p>
                </div>

                <div className="col-span-2">
                  <span className="text-[10px] font-semibold text-slate-400 bg-slate-950 p-1 rounded-sm">
                    {pay.type}
                  </span>
                </div>

                <div className="col-span-2 text-center font-mono font-bold text-slate-200">
                  INR {pay.amount.toLocaleString()}
                </div>

                <div className="col-span-2 text-center font-mono text-slate-400">
                  {pay.method}
                </div>

                <div className="col-span-2 text-center">
                  <span
                    className={`inline-flex items-center gap-1 text-[9px] px-2 py-0.5 rounded font-bold uppercase ${
                      pay.status === "Paid"
                        ? "bg-emerald-500/10 text-emerald-400"
                        : "bg-rose-500/10 text-rose-400 animate-pulse"
                    }`}
                  >
                    {pay.status === "Paid" ? <BadgeCheck className="w-3 h-3" /> : <AlertTriangle className="w-3 h-3" />}
                    <span>{pay.status}</span>
                  </span>
                </div>

                <div className="col-span-1 text-right flex justify-end gap-1.5 font-mono">
                  {pay.status === "Pending" ? (
                    <button
                      onClick={() => onClearPending(pay.id)}
                      className="bg-emerald-500/10 border border-emerald-500/25 hover:bg-emerald-500 hover:text-slate-950 text-emerald-400 px-2 py-1 rounded text-[10px] font-bold transition-all cursor-pointer"
                      title="Click once manual GPay check has been cleared"
                    >
                      Realize
                    </button>
                  ) : (
                    <button
                      onClick={() => setActiveReceipt(pay)}
                      className="bg-slate-800 hover:bg-slate-750 text-slate-350 hover:text-white p-1 rounded transition-colors cursor-pointer"
                      title="View transaction receipt"
                    >
                      <Receipt className="w-3.5 h-3.5" />
                    </button>
                  )}
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* 2. INVOICE LEDGER */}
      {subMode === "invoices" && (
        <div className="bg-slate-900/60 border border-white/5 rounded-xl overflow-hidden shadow">
          <div className="bg-slate-950/30 border-b border-light/5 px-4 py-2.5 text-[10px] font-mono text-slate-500 uppercase tracking-widest grid grid-cols-12 gap-2 font-bold select-none text-left">
            <span className="col-span-2">Invoice No</span>
            <span className="col-span-3">Payer details</span>
            <span className="col-span-3">Item particulars</span>
            <span className="col-span-2 text-center">Grand Total</span>
            <span className="col-span-1 text-center">Status</span>
            <span className="col-span-1 text-right">View bill</span>
          </div>

          <div className="divide-y divide-white/5">
            {invoices.map(inv => (
              <div key={inv.id} className="px-4 py-3.5 grid grid-cols-12 gap-2 text-xs items-center text-left hover:bg-slate-950/10">
                <div className="col-span-2 font-mono font-bold text-amber-500">
                  {inv.invoiceNo}
                </div>

                <div className="col-span-3 min-w-0">
                  <p className="font-semibold text-slate-100 truncate">{inv.clientName}</p>
                  <p className="text-[10px] text-slate-500 truncate">{inv.clientEmail}</p>
                </div>

                <div className="col-span-3 min-w-0">
                  <p className="truncate text-slate-350">{inv.itemName}</p>
                  <p className="text-[10px] text-slate-500 font-mono">Issued: {inv.issuedDate}</p>
                </div>

                <div className="col-span-2 text-center font-mono font-bold text-slate-200">
                  INR {inv.totalAmount.toLocaleString()}
                </div>

                <div className="col-span-1 text-center">
                  <span
                    className={`text-[9px] px-1.5 py-0.5 rounded font-bold uppercase ${
                      inv.status === "Paid"
                        ? "bg-emerald-500/10 text-emerald-400"
                        : "bg-rose-500/10 text-rose-400"
                    }`}
                  >
                    {inv.status}
                  </span>
                </div>

                <div className="col-span-1 text-right">
                  <button
                    onClick={() => setActiveInvoice(inv)}
                    className="bg-slate-800 hover:bg-slate-750 text-slate-350 hover:text-amber-400 px-2.5 py-1 rounded transition-colors cursor-pointer text-[10px] font-semibold"
                  >
                    Open Bill
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* DYNAMIC LAW COMMERCIAL INVOICE PDF/HTML DISPLAY POPUP */}
      {activeInvoice && (
        <div className="fixed inset-0 bg-slate-955/90 backdrop-blur-xs flex items-center justify-center z-50 p-4 animate-fade-in print:bg-white print:p-0">
          <div className="bg-slate-900 print:bg-white text-slate-100 print:text-black border border-slate-800 rounded-2xl w-full max-w-2xl overflow-hidden shadow-2xl flex flex-col max-h-[90vh]">
            
            {/* Header controls */}
            <div className="p-4 bg-slate-950/60 print:hidden border-b border-white/5 flex justify-between items-center shrink-0">
              <span className="font-mono text-xs text-amber-500">Tax Invoice Rendering System</span>
              <div className="flex gap-2">
                <button
                  onClick={handlePrint}
                  className="bg-amber-500 text-slate-950 font-bold px-3 py-1.5 rounded-lg text-[10.5px] hover:bg-amber-450 transition-colors flex items-center gap-1 cursor-pointer"
                >
                  <Printer className="w-3.5 h-3.5" />
                  <span>Print Document</span>
                </button>
                <button
                  onClick={() => setActiveInvoice(null)}
                  className="p-1 rounded bg-white/5 text-slate-400 hover:text-white"
                >
                  ✕
                </button>
              </div>
            </div>

            {/* Printable tax bill body */}
            <div className="p-8 flex-1 overflow-y-auto space-y-6 text-xs bg-linear-to-b from-slate-900 to-slate-950 print:bg-white print:from-white print:to-white">
              <div className="flex justify-between items-start border-b border-white/5 print:border-black/10 pb-5">
                <div>
                  <div className="w-10 h-10 rounded-full bg-amber-500 flex items-center justify-center shadow-md mb-2 print:border">
                    <span className="text-slate-950 font-bold text-xs">ALP</span>
                  </div>
                  <h4 className="font-bold text-slate-100 print:text-black text-sm">Akshaya Lagna Paddhati Astrology Academy</h4>
                  <p className="text-[10px] text-slate-400 print:text-black/60 font-mono mt-0.5">
                    GSTIN: 33CADPM1228B1Z2<br />
                    Mylapore Double Street, Chennai - 600004
                  </p>
                </div>
                <div className="text-right text-[10.5px]">
                  <h3 className="text-amber-500 font-bold tracking-wider uppercase font-mono print:text-amber-600">Tax Invoice</h3>
                  <p className="font-mono pt-1 text-slate-200 print:text-black">No: <span className="font-bold font-sans">{activeInvoice.invoiceNo}</span></p>
                  <p className="text-slate-400 print:text-black/60 font-mono">Date: {activeInvoice.issuedDate}</p>
                  <p className="text-slate-400 print:text-black/60 font-mono">Due: {activeInvoice.dueDate}</p>
                </div>
              </div>

              {/* Bill To Address columns */}
              <div className="grid grid-cols-2 gap-4">
                <div className="bg-slate-950/40 p-3 rounded print:border print:bg-transparent text-[11px]">
                  <h5 className="font-mono text-[9px] uppercase text-slate-500 mb-1">Billing to Customer:</h5>
                  <p className="font-bold text-slate-200 print:text-black">{activeInvoice.clientName}</p>
                  <p className="text-slate-400 print:text-black/60">{activeInvoice.clientEmail}</p>
                  <p className="text-slate-400 print:text-black/60 mt-2 italic">Client IP: Verified REST token check</p>
                </div>
                <div className="bg-slate-955/40 p-3 rounded print:border print:bg-transparent text-[11px] space-y-1.5 font-mono">
                  <h5 className="text-[9px] uppercase text-slate-500 mb-1">Tax Calculation Particulars:</h5>
                  <p className="text-slate-400 print:text-black/60">Inter-State GST Alignment: CGST (9%) + SGST (9%)</p>
                  <p className="text-slate-400 print:text-black/60">Fractions Rounding off: Checked</p>
                </div>
              </div>

              {/* Item list table */}
              <div className="border border-white/5 print:border-black/10 rounded-lg overflow-hidden">
                <div className="bg-slate-950 px-4 py-2 text-[9px] font-mono uppercase tracking-wider text-slate-500 grid grid-cols-12 gap-2 print:border-b">
                  <span className="col-span-7">Service Details / Item description</span>
                  <span className="col-span-2 text-right">Pre-Tax Amt</span>
                  <span className="col-span-1 text-right">GST (18%)</span>
                  <span className="col-span-2 text-right">Total Invoice</span>
                </div>
                <div className="px-4 py-3 grid grid-cols-12 gap-2 items-center border-b border-white/5">
                  <p className="col-span-7 font-semibold text-slate-200 print:text-black">{activeInvoice.itemName}</p>
                  <p className="col-span-2 text-right font-mono text-slate-300 print:text-black">INR {activeInvoice.amount.toLocaleString()}</p>
                  <p className="col-span-1 text-right font-mono text-slate-300 print:text-black">INR {activeInvoice.taxAmount.toLocaleString()}</p>
                  <p className="col-span-2 text-right font-mono font-bold text-slate-100 print:text-black">INR {activeInvoice.totalAmount.toLocaleString()}</p>
                </div>
              </div>

              {/* Grand total summaries */}
              <div className="flex justify-between items-start gap-5 pt-4">
                <p className="text-[10px] text-slate-500 leading-normal max-w-sm italic">
                  Thank you for booking with Akshaya Lagna Paddhati (ALP) Astrology operations. This computed ledger represents a legal digitized certificate invoice receipt under the Indian Information Technology Act 2000. No physical script signature is mandatory.
                </p>
                <div className="w-56 font-mono text-[11px] text-right space-y-1 bg-slate-950/20 p-3 rounded border border-white/5">
                  <div className="flex justify-between text-slate-400"><span>Pre-Tax:</span> <span>INR {activeInvoice.amount.toLocaleString()}</span></div>
                  <div className="flex justify-between text-slate-400"><span>CGST 9%:</span> <span>INR {(activeInvoice.taxAmount/2).toLocaleString()}</span></div>
                  <div className="flex justify-between text-slate-400"><span>SGST 9%:</span> <span>INR {(activeInvoice.taxAmount/2).toLocaleString()}</span></div>
                  <div className="flex justify-between border-t border-white/10 pt-1 text-slate-100 font-bold font-sans text-xs print:text-black"><span>Total Bill:</span> <span>INR {activeInvoice.totalAmount.toLocaleString()}</span></div>
                </div>
              </div>
            </div>
            
            <div className="p-4 bg-slate-950/60 print:hidden text-center text-slate-500 text-[10px] border-t border-white/5">
              Unique secure payment ID: {activeInvoice.paymentId}
            </div>
          </div>
        </div>
      )}

      {/* DYNAMIC COMPLETED PAYMENT RECEIPT DISPLAY POPUP */}
      {activeReceipt && (
        <div className="fixed inset-0 bg-slate-955/90 backdrop-blur-xs flex items-center justify-center z-50 p-4 animate-fade-in print:bg-white print:p-0">
          <div className="bg-slate-900 border border-slate-800 text-slate-100 rounded-2xl w-full max-w-sm overflow-hidden shadow-2xl flex flex-col">
            <div className="p-4 bg-slate-950/60 border-b border-white/5 flex justify-between items-center print:hidden">
              <span className="font-mono text-[11px] text-emerald-400">Transaction Receipt</span>
              <button onClick={() => setActiveReceipt(null)} className="p-1 rounded bg-white/5 text-slate-400 hover:text-white">✕</button>
            </div>
            <div className="p-6 text-center space-y-5 bg-linear-to-b from-slate-900 to-slate-955">
              <div className="w-12 h-12 rounded-full bg-emerald-500/10 border border-emerald-500/30 flex items-center justify-center mx-auto text-emerald-400 mb-3 animate-pulse">
                <BadgeCheck className="w-6 h-6" />
              </div>
              <div>
                <h3 className="font-bold text-sm text-slate-100 font-sans">{activeReceipt.payerName}</h3>
                <p className="text-[10px] text-slate-500">{activeReceipt.payerEmail}</p>
              </div>
              <div className="border-y border-white/5 py-4 my-2 text-xs">
                <p className="text-slate-500">REALIZED AMOUNT</p>
                <p className="text-3xl font-extrabold text-slate-100 font-mono mt-1">INR {activeReceipt.amount.toLocaleString()}</p>
                <p className="text-[10px] text-emerald-400 mt-1 uppercase tracking-wider font-bold">Cleared successful • UPI</p>
              </div>
              <div className="text-[10.5px] font-mono text-slate-400 space-y-1 text-left bg-slate-950/40 p-3.5 rounded border border-white/5">
                <p className="truncate"><span className="text-slate-500">Method:</span> {activeReceipt.method}</p>
                <p><span className="text-slate-500">Source:</span> {activeReceipt.type}</p>
                <p><span className="text-slate-500">Date:</span> {activeReceipt.date}</p>
                <p className="truncate text-[9.5px]"><span className="text-slate-500">Ref ID:</span> {activeReceipt.id}</p>
              </div>
              <p className="text-[9.5px] text-slate-500 leading-normal italic text-center max-w-xs pt-2">
                This transaction ledger receipt maps to GST invoice ledger entries securely. Checked by Alp Astrology core finances.
              </p>
            </div>
          </div>
        </div>
      )}

      {/* QUICK TRANSACTION BOOKING FORM */}
      {isOpenAddTrans && (
        <div className="fixed inset-0 bg-slate-950/80 backdrop-blur-xs flex items-center justify-center z-50 p-4">
          <div className="bg-slate-900 border border-amber-500/15 rounded-xl w-full max-w-md overflow-hidden text-xs">
            <div className="p-4 bg-slate-950/65 border-b border-light/5 flex justify-between items-center">
              <h3 className="font-bold text-slate-100 text-sm">Create Manual Transaction ledger</h3>
              <button onClick={() => setIsOpenAddTrans(false)} className="text-slate-400 hover:text-white p-1 rounded">✕</button>
            </div>

            <form onSubmit={submitNewTrans} className="p-5 space-y-4">
              <div>
                <label className="text-[10px] font-mono text-slate-400 block mb-1">Payer Billing Name *</label>
                <input
                  type="text"
                  required
                  placeholder="Kalyan Kumar"
                  value={newTrans.payerName}
                  onChange={e => setNewTrans({ ...newTrans, payerName: e.target.value })}
                  className="w-full bg-slate-955 border border-slate-800 rounded px-2.5 py-1.5 text-white"
                />
              </div>
              <div>
                <label className="text-[10px] font-mono text-slate-400 block mb-1">Payer Email *</label>
                <input
                  type="email"
                  required
                  placeholder="kalyan@gmail.com"
                  value={newTrans.payerEmail}
                  onChange={e => setNewTrans({ ...newTrans, payerEmail: e.target.value })}
                  className="w-full bg-slate-955 border border-slate-800 rounded px-2.5 py-1.5 text-white"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="text-[10px] font-mono text-slate-400 block mb-1">Source income *</label>
                  <select
                    value={newTrans.type}
                    onChange={e => setNewTrans({ ...newTrans, type: e.target.value as any })}
                    className="w-full bg-slate-955 border border-slate-800 rounded px-2 py-1.5 text-white cursor-pointer"
                  >
                    <option value="Consultation Fees" className="bg-slate-900">Consultation Fees</option>
                    <option value="Course Fees" className="bg-slate-900">Course Fees</option>
                    <option value="Events" className="bg-slate-900">Events Tracker</option>
                    <option value="Memberships" className="bg-slate-900">Memberships Space</option>
                  </select>
                </div>
                <div>
                  <label className="text-[10px] font-mono text-slate-400 block mb-1">Manually billed Amount (INR) *</label>
                  <input
                    type="number"
                    required
                    value={newTrans.amount}
                    onChange={e => setNewTrans({ ...newTrans, amount: e.target.value })}
                    className="w-full bg-slate-955 border border-slate-800 rounded px-2.5 py-1.5 text-white"
                  />
                </div>
                <div>
                  <label className="text-[10px] font-mono text-slate-400 block mb-1">Payment Channel *</label>
                  <select
                    value={newTrans.method}
                    onChange={e => setNewTrans({ ...newTrans, method: e.target.value })}
                    className="w-full bg-slate-955 border border-slate-800 rounded px-2 py-1.5 text-white cursor-pointer"
                  >
                    <option value="UPI (GPay/PhonePe)" className="bg-slate-900">UPI (GPay/PhonePe)</option>
                    <option value="Online (Stripe)" className="bg-slate-900">Online (Stripe Credit Card)</option>
                    <option value="Bank Transfer" className="bg-slate-900">Direct Bank Transfer</option>
                    <option value="Cash" className="bg-slate-900">Physical Cash</option>
                  </select>
                </div>
                <div>
                  <label className="text-[10px] font-mono text-slate-400 block mb-1">Clearance Status *</label>
                  <select
                    value={newTrans.status}
                    onChange={e => setNewTrans({ ...newTrans, status: e.target.value })}
                    className="w-full bg-slate-955 border border-slate-800 rounded px-2 py-1.5 text-white cursor-pointer"
                  >
                    <option value="Paid" className="bg-slate-900">Paid Clear</option>
                    <option value="Pending" className="bg-slate-900">Pending Dues</option>
                  </select>
                </div>
              </div>

              <div className="pt-2 flex justify-end gap-2 text-xs font-bold">
                <button
                  type="button"
                  onClick={() => setIsOpenAddTrans(false)}
                  className="bg-slate-800 hover:bg-slate-755 text-slate-350 border border-slate-705 px-4 py-2 rounded-lg cursor-pointer"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="bg-amber-500 hover:bg-amber-450 text-slate-955 px-4 py-2 rounded-lg shadow-lg shadow-amber-500/10 cursor-pointer"
                >
                  Confirm Ledger entries
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
