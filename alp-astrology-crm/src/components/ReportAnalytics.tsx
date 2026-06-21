import React from "react";
import {
  TrendingUp,
  LineChart,
  BarChart2,
  DollarSign,
  Users,
  Award,
  Sparkles,
  PieChart,
  Activity,
  ArrowUpRight
} from "lucide-react";

export function ReportAnalytics() {
  const [stats, setStats] = React.useState<{
    revenueSummary: { title: string; current: number; previous: number; rate: string }[];
    conversionFunnel: { stage: string; count: number; percentage: number }[];
    incomeChannels: { category: string; value: number }[];
    academicUtilization: { batchName: string; capacity: number; enrolled: number }[];
    trainerRatings: { name: string; score: number; lessonsCount: number }[];
  } | null>(null);

  const [isLoading, setIsLoading] = React.useState(true);

  React.useEffect(() => {
    setIsLoading(true);
    fetch("/api/reports/analytics")
      .then(res => res.json())
      .then(data => {
        setStats(data);
      })
      .catch(err => console.error("Error loading analytical metrics", err))
      .finally(() => setIsLoading(false));
  }, []);

  if (isLoading || !stats) {
    return (
      <div className="text-center py-24 font-mono text-xs text-slate-400 space-y-3">
        <Activity className="w-8 h-8 text-amber-500 animate-spin mx-auto" />
        <p>Structuring financial conversion vectors...</p>
      </div>
    );
  }

  // Calculate coordinates for SVG diagrams
  const maxCap = Math.max(...stats.academicUtilization.map(u => u.capacity));

  return (
    <div className="space-y-6">
      {/* 3 Grid Premium Indicators Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        {stats.revenueSummary.map((sum, index) => (
          <div key={index} className="bg-slate-900 border border-slate-800 rounded-xl p-5 space-y-3 text-xs shadow-md">
            <div className="flex justify-between items-center text-slate-500 font-mono text-[10px] uppercase">
              <span>{sum.title}</span>
              <span className="text-emerald-400 bg-emerald-500/5 px-2 py-0.5 rounded-full flex items-center gap-0.5">
                <ArrowUpRight className="w-3 h-3" />
                <span>{sum.rate}</span>
              </span>
            </div>

            <div className="space-y-1">
              <h3 className="text-2xl font-black text-slate-100 font-mono">INR {sum.current.toLocaleString()}</h3>
              <p className="text-[10px] text-slate-400 font-mono font-semibold">Previous audit period: INR {sum.previous.toLocaleString()}</p>
            </div>
          </div>
        ))}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        
        {/* Left Column (7 Units) - Conversion Funnel and Batch fill rates */}
        <div className="lg:col-span-7 space-y-6">
          {/* Conversion Leads Funnel bar diagram */}
          <div className="bg-slate-900 border border-white/5 rounded-xl p-5 shadow">
            <h3 className="font-bold text-sm text-slate-200 mb-4 flex items-center gap-1.5">
              <TrendingUp className="w-4 h-4 text-amber-500" />
              <span>Lead Acquisition Conversion Funnel Matrix</span>
            </h3>

            <div className="space-y-4">
              {stats.conversionFunnel.map((fun, fnIdx) => {
                // Width calculation
                const wPct = fun.percentage;
                return (
                  <div key={fnIdx} className="space-y-1.5 text-xs">
                    <div className="flex justify-between font-mono text-[10.5px]">
                      <span className="font-bold text-slate-350">{fun.stage}</span>
                      <span className="text-slate-450">{fun.count} Leads ({fun.percentage}%)</span>
                    </div>
                    {/* Visual Bar */}
                    <div className="h-5 bg-slate-950 rounded overflow-hidden p-0.5 border border-white/5">
                      <div
                        style={{ width: `${wPct}%` }}
                        className="h-full bg-linear-to-r from-amber-500/75 to-yellow-500/60 rounded flex items-center pl-2 text-[9px] font-bold text-slate-950 font-mono transition-all duration-1000"
                      >
                        {fun.percentage}% conversion rate
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>

          {/* Classroom Fill Rates */}
          <div className="bg-slate-900 border border-white/5 rounded-xl p-5 shadow">
            <h3 className="font-bold text-sm text-slate-200 mb-4 flex items-center gap-1.5">
              <Users className="w-4 h-4 text-emerald-400" />
              <span>Syllabus Batch Enrollment Capacity Ratios</span>
            </h3>

            <div className="space-y-3">
              {stats.academicUtilization.map((ut, utIdx) => {
                const ratio = Math.round((ut.enrolled / ut.capacity) * 100);
                return (
                  <div key={utIdx} className="p-3 bg-slate-950/40 rounded-lg border border-white/5 flex justify-between items-center text-xs">
                    <div>
                      <p className="font-bold text-slate-200">{ut.batchName}</p>
                      <p className="text-[10px] text-slate-500 font-mono mt-0.5">Capacity capacity lines: {ut.capacity} Max seats</p>
                    </div>

                    <div className="flex items-center gap-4">
                      <span className="text-slate-400 font-mono text-[10.5px]">{ut.enrolled} Students Enrolled</span>
                      <div className="w-12 h-12 rounded-full border-4 border-slate-800 flex items-center justify-center text-[10.5px] font-bold text-amber-500 font-mono">
                        {ratio}%
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        </div>

        {/* Right Column (5 Units) - Revenue shares and Trainer performances */}
        <div className="lg:col-span-5 space-y-6">
          {/* Revenue Category Breakdown */}
          <div className="bg-slate-900 border border-white/5 rounded-xl p-5 shadow">
            <h3 className="font-bold text-sm text-slate-200 mb-4 flex items-center gap-1.5">
              <DollarSign className="w-4 h-4 text-amber-400" />
              <span>SaaS Channels Income Share</span>
            </h3>

            <div className="space-y-3 text-xs">
              {stats.incomeChannels.map((chn, cIdx) => (
                <div key={cIdx} className="flex justify-between items-center py-2 border-b border-white/5">
                  <div className="flex items-center gap-2">
                    <span className="w-2 h-2 rounded-full bg-amber-500"></span>
                    <span className="font-semibold text-slate-300">{chn.category}</span>
                  </div>
                  <span className="font-mono font-bold text-slate-100">INR {chn.value.toLocaleString()}</span>
                </div>
              ))}
            </div>
          </div>

          {/* Trainer ratings list */}
          <div className="bg-slate-900 border border-white/5 rounded-xl p-5 shadow">
            <h3 className="font-bold text-sm text-slate-200 mb-4 flex items-center gap-1.5">
              <Award className="w-4 h-4 text-purple-400" />
              <span>Faculty Astrologers Performance Ratings</span>
            </h3>

            <div className="space-y-3 text-xs">
              {stats.trainerRatings.map((tr, trIdx) => (
                <div key={trIdx} className="p-3 bg-slate-950/40 rounded-lg border border-white/5 flex justify-between items-center">
                  <div>
                    <span className="font-bold text-slate-200 block">{tr.name}</span>
                    <span className="text-[10px] text-slate-500 font-mono">Active Lessons: {tr.lessonsCount} Classes</span>
                  </div>

                  <div className="text-right font-mono">
                    <span className="text-amber-400 font-extrabold text-xs">★ {tr.score} / 5.0</span>
                    <p className="text-[9.5px] text-slate-500">Positive Feedback</p>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
