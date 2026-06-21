import express from "express";
import path from "path";
import { createServer as createViteServer } from "vite";
import { GoogleGenAI } from "@google/genai";
import {
  User,
  UserRole,
  Lead,
  LeadActivity,
  Client,
  Consultation,
  ConsultationNote,
  Course,
  Batch,
  Student,
  StudentEnrollment,
  Payment,
  Invoice,
  Certificate,
  Event,
  EventRegistration,
  CommunityMember,
  SocialMediaPost,
  AuditLog,
  Task,
  Notification,
  Employee
} from "./src/types";

// Setup Server
const app = express();
const PORT = 3000;

// Enable JSON bodies
app.use(express.json());

// ==========================================
// CENTRALIZED IN-MEMORY DATABASES (SEED DATA)
// ==========================================

let currentUser: User = {
  id: "emp-1",
  name: "Dr. K. Muralidharan (Founder)",
  email: "founder@alpastrology.com",
  role: "Super Admin",
  phone: "+91 98400 12345",
  avatar: "https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=150"
};

let employees: Employee[] = [
  {
    id: "emp-1",
    name: "Dr. K. Muralidharan (Founder)",
    email: "founder@alpastrology.com",
    role: "Super Admin",
    phone: "+91 98400 12345",
    password: "admin",
    avatar: "https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=150",
    createdTime: "2026-01-15T09:00:00Z",
    status: "Active"
  },
  {
    id: "emp-2",
    name: "Aarti Sharma (Employee 1)",
    email: "employee1@alpastrology.com",
    role: "Astrologer",
    phone: "+91 98400 54321",
    password: "employee1",
    avatar: "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150",
    createdTime: "2026-03-10T10:30:00Z",
    status: "Active"
  },
  {
    id: "emp-3",
    name: "Suresh Iyer (Employee 2)",
    email: "employee2@alpastrology.com",
    role: "Receptionist",
    phone: "+91 98300 11223",
    password: "employee2",
    avatar: "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150",
    createdTime: "2026-04-01T11:00:00Z",
    status: "Active"
  },
  {
    id: "emp-4",
    name: "Rohan Gupta (Employee 3)",
    email: "employee3@alpastrology.com",
    role: "Social Media Manager",
    phone: "+91 98200 44556",
    password: "employee3",
    avatar: "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150",
    createdTime: "2026-05-18T14:15:00Z",
    status: "Active"
  }
];

const defaultRoles = [
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

// Leads database
let leads: Lead[] = [
  {
    id: "lead-1",
    name: "Srinivasan Raman",
    mobile: "+91 94440 56789",
    email: "srinivasan.r@gmail.com",
    country: "India",
    city: "Chennai",
    language: "Tamil",
    source: "Website",
    interestType: "Consultation",
    status: "New Lead",
    notes: "Wants premium career astrology consultation for his son's foreign placement.",
    assignedStaff: "Astrologer Ramanujan",
    createdAt: "2026-06-18T10:30:00Z",
    updatedAt: "2026-06-18T10:30:00Z"
  },
  {
    id: "lead-2",
    name: "Aishwarya Rajesh",
    mobile: "+91 98845 11223",
    email: "aishwarya.rajesh@outlook.com",
    country: "India",
    city: "Coimbatore",
    language: "Tamil",
    source: "Instagram",
    interestType: "Course",
    status: "Contacted",
    notes: "Enquired about the ALP Advanced Course. Ready for evening batches.",
    assignedStaff: "Trainer Sathyabhama",
    createdAt: "2026-06-19T09:15:00Z",
    updatedAt: "2026-06-19T11:40:00Z"
  },
  {
    id: "lead-3",
    name: "Dr. Anand Kumar",
    mobile: "+1 408 555 0192",
    email: "anand.k@stanford.edu",
    country: "United States",
    city: "San Jose",
    language: "English",
    source: "YouTube",
    interestType: "Consultation",
    status: "Follow-up Required",
    notes: "Requires business partnership compatibility analysis using Akshaya Lagna Paddhati.",
    assignedStaff: "Astrologer Balakrishnan",
    createdAt: "2026-06-15T14:20:00Z",
    updatedAt: "2026-06-17T16:45:00Z"
  },
  {
    id: "lead-4",
    name: "Meenakshi Sundaram",
    mobile: "+91 95000 88776",
    email: "meenakshi.s@yahoo.com",
    country: "India",
    city: "Madurai",
    language: "Tamil",
    source: "WhatsApp",
    interestType: "Consultation",
    status: "Consultation Booked",
    notes: "Booked marriage matching consultation for 22nd June.",
    assignedStaff: "Receptionist Priya",
    createdAt: "2026-06-19T08:00:00Z",
    updatedAt: "2026-06-19T15:30:00Z"
  },
  {
    id: "lead-5",
    name: "Vikram Shah",
    mobile: "+91 91234 56789",
    email: "vikram.shah@fintech.co",
    country: "India",
    city: "Mumbai",
    language: "English",
    source: "Facebook",
    interestType: "Course",
    status: "Interested",
    notes: "Fintech executive interested in learning ALP Professional Course for self-timing of equity trades.",
    assignedStaff: "Trainer Sathyabhama",
    createdAt: "2026-06-20T04:30:00Z",
    updatedAt: "2026-06-20T05:00:00Z"
  }
];

// Lead activities
let leadActivities: LeadActivity[] = [
  {
    id: "act-1",
    leadId: "lead-1",
    staffName: "Receptionist Priya",
    actionType: "Note Added",
    details: "Lead created automatically from Akshaya Astrology Website contact form.",
    timestamp: "2026-06-18T10:30:00Z"
  },
  {
    id: "act-2",
    leadId: "lead-2",
    staffName: "Trainer Sathyabhama",
    actionType: "WhatsApp",
    details: "Sent ALP Advanced Course syllabus details via WhatsApp business trigger.",
    timestamp: "2026-06-19T11:40:00Z"
  },
  {
    id: "act-3",
    leadId: "lead-3",
    staffName: "Astrologer Balakrishnan",
    actionType: "Call",
    details: "Client asked to postpone discussion to Sunday evening due to timezone travel conflicts.",
    timestamp: "2026-06-17T16:45:00Z"
  },
  {
    id: "act-4",
    leadId: "lead-4",
    staffName: "Receptionist Priya",
    actionType: "Status Change",
    details: "Configured consultation slot. Confirmed payment checkout for marriage counseling.",
    timestamp: "2026-06-19T15:30:00Z"
  }
];

// Clients database
let clients: Client[] = [
  {
    id: "client-1",
    name: "Meenakshi Sundaram",
    phone: "+91 95000 88776",
    email: "meenakshi.s@yahoo.com",
    dob: "1994-04-12",
    birthTime: "14:35",
    birthPlace: "Madurai, Tamil Nadu",
    address: "42, West Masi Street, Madurai - 625001",
    occupation: "Software Engineer",
    country: "India",
    language: "Tamil"
  },
  {
    id: "client-2",
    name: "Arun Swaminathan",
    phone: "+91 98404 99887",
    email: "aruns@outlook.com",
    dob: "1988-11-23",
    birthTime: "05:42",
    birthPlace: "Tiruchirappalli, Tamil Nadu",
    address: "12B, Salai Road, Woraiyur, Trichy - 620003",
    occupation: "Retail Trader",
    country: "India",
    language: "Tamil"
  },
  {
    id: "client-3",
    name: "Pooja Hegde",
    phone: "+1 650 934 8110",
    email: "pooja.hegde@google.com",
    dob: "1997-07-19",
    birthTime: "08:15",
    birthPlace: "Mangalore, Karnataka",
    address: "320 Twin Peaks Blvd, San Francisco, CA 94114",
    occupation: "Product Manager",
    country: "United States",
    language: "English"
  }
];

// Consultations database
let consultations: Consultation[] = [
  {
    id: "cons-1",
    clientId: "client-1",
    clientName: "Meenakshi Sundaram",
    type: "Marriage",
    dateTime: "2026-06-22T10:00:00.000Z",
    status: "Scheduled",
    astrologerId: "astro-ramanujan",
    astrologerName: "Astrologer Ramanujan",
    meetingLink: "https://meet.google.com/abc-defg-hij",
    followUpDate: "2026-07-22",
    followUpNotes: "Review post-remedy performance inside 30 days."
  },
  {
    id: "cons-2",
    clientId: "client-2",
    clientName: "Arun Swaminathan",
    type: "Business",
    dateTime: "2026-06-19T16:00:00.000Z",
    status: "Completed",
    astrologerId: "astro-muralidharan",
    astrologerName: "Dr. K. Muralidharan",
    meetingLink: "https://meet.google.com/xyz-uvwx-123",
    recordingLink: "https://drive.google.com/file/d/alp-rec-98242",
    followUpDate: "2026-08-15",
    followUpNotes: "Timing of new store launch is critical. Check Jupiter transit."
  },
  {
    id: "cons-3",
    clientId: "client-3",
    clientName: "Pooja Hegde",
    type: "Career",
    dateTime: "2026-06-25T18:30:00.000Z",
    status: "Scheduled",
    astrologerId: "astro-balakrishnan",
    astrologerName: "Astrologer Balakrishnan",
    meetingLink: "https://zoom.us/j/9081234567"
  }
];

// Consultation Notes
let consultationNotes: ConsultationNote[] = [
  {
    id: "note-1",
    consultationId: "cons-2",
    writerName: "Dr. K. Muralidharan",
    title: "ALP Lagna Assessment",
    content: "Born in Scorpio Lagna. Current ALP (Akshaya Lagna Paddhati) progressed lagna is in Taurus. Taurus is ruled by Venus. Progressed lagna 10th house falls in Aquarius, containing Saturn. Indicates major brick-and-mortar capital deployment. Advised to postpone trade agreements until September 14th transit.",
    timestamp: "2026-06-19T17:15:00Z"
  }
];

// Courses
let courses: Course[] = [
  {
    id: "c-1",
    name: "ALP Astrology Basic Course",
    code: "ALP-BAS-101",
    description: "Introduction to Akshaya Lagna Paddhati base calculations, mathematical houses mapping, and foundational transit timings.",
    durationWeeks: 8,
    fees: 15000
  },
  {
    id: "c-2",
    name: "ALP Advanced Timing Techniques",
    code: "ALP-ADV-202",
    description: "Deep dive analysis of Vimshottari Dasha vs progressed ALP Lagna house triggers to time career peaks and material blessings.",
    durationWeeks: 12,
    fees: 25000
  },
  {
    id: "c-3",
    name: "ALP Professional Astrologer Certification",
    code: "ALP-PRO-303",
    description: "A fast-track curriculum for practicing astrologers looking to upgrade consultations to 99% timing precision using ALP methodology.",
    durationWeeks: 16,
    fees: 45000
  },
  {
    id: "c-4",
    name: "ALP Research Fellowship Program",
    code: "ALP-RES-404",
    description: "Collaborative astrology research program analyzing medical astrology, stock market timing, and global geopolitical triggers.",
    durationWeeks: 24,
    fees: 60000
  }
];

// Batches
let batches: Batch[] = [
  {
    id: "b-1",
    courseId: "c-1",
    courseName: "ALP Astrology Basic Course",
    name: "Saturday Tamil Batch - Q3",
    trainerId: "trainer-sathyabhama",
    trainerName: "Trainer Sathyabhama",
    startDate: "2026-07-04",
    schedule: "Saturdays 18:00 - 20:00 IST",
    status: "Upcoming"
  },
  {
    id: "b-2",
    courseId: "c-2",
    courseName: "ALP Advanced Timing Techniques",
    name: "Sunday English Global Batch - Q2",
    trainerId: "trainer-srinivasan",
    trainerName: "Trainer Srinivasan",
    startDate: "2026-05-10",
    schedule: "Sundays 07:00 - 09:00 PST",
    status: "Active"
  }
];

// Students database
let students: Student[] = [
  {
    id: "stud-1",
    name: "Balaji Swaminathan",
    phone: "+91 94432 00192",
    email: "balaji.swami@gmail.com",
    country: "India",
    occupation: "Senior Consultant",
    joinedDate: "2026-04-12"
  },
  {
    id: "stud-2",
    name: "Subhashini Sridhar",
    phone: "+91 98844 76654",
    email: "subha.sridhar@gmail.com",
    country: "India",
    occupation: "Lecturer",
    joinedDate: "2026-05-01"
  },
  {
    id: "stud-3",
    name: "Karthik Raja",
    phone: "+65 9182 7364",
    email: "karthikr@gmail.com",
    country: "Singapore",
    occupation: "Financial Analyst",
    joinedDate: "2026-05-05"
  }
];

// Student Enrollments
let studentEnrollments: StudentEnrollment[] = [
  {
    id: "enr-1",
    studentId: "stud-1",
    studentName: "Balaji Swaminathan",
    courseId: "c-1",
    courseName: "ALP Astrology Basic Course",
    batchId: "b-1",
    batchName: "Saturday Tamil Batch - Q3",
    enrollmentDate: "2026-06-15",
    progressPercentage: 10,
    attendancePercentage: 100,
    status: "In Progress"
  },
  {
    id: "enr-2",
    studentId: "stud-2",
    studentName: "Subhashini Sridhar",
    courseId: "c-2",
    courseName: "ALP Advanced Timing Techniques",
    batchId: "b-2",
    batchName: "Sunday English Global Batch - Q2",
    enrollmentDate: "2026-05-08",
    progressPercentage: 45,
    attendancePercentage: 92,
    status: "In Progress"
  },
  {
    id: "enr-3",
    studentId: "stud-3",
    studentName: "Karthik Raja",
    courseId: "c-2",
    courseName: "ALP Advanced Timing Techniques",
    batchId: "b-2",
    batchName: "Sunday English Global Batch - Q2",
    enrollmentDate: "2026-05-09",
    progressPercentage: 50,
    attendancePercentage: 100,
    status: "In Progress"
  }
];

// Payments
let payments: Payment[] = [
  {
    id: "pay-1",
    payerName: "Meenakshi Sundaram",
    payerEmail: "meenakshi.s@yahoo.com",
    type: "Consultation Fees",
    amount: 3000,
    method: "UPI (GPay/PhonePe)",
    status: "Paid",
    date: "2026-06-19",
    invoiceId: "inv-1"
  },
  {
    id: "pay-2",
    payerName: "Balaji Swaminathan",
    payerEmail: "balaji.swami@gmail.com",
    type: "Course Fees",
    amount: 15000,
    method: "Bank Transfer",
    status: "Paid",
    date: "2026-06-15",
    invoiceId: "inv-2"
  },
  {
    id: "pay-3",
    payerName: "Karthik Raja",
    payerEmail: "karthikr@gmail.com",
    type: "Course Fees",
    amount: 25000,
    method: "Online (Stripe)",
    status: "Paid",
    date: "2026-05-09",
    invoiceId: "inv-3"
  },
  {
    id: "pay-4",
    payerName: "Aishwarya Rajesh",
    payerEmail: "aishwarya.rajesh@outlook.com",
    type: "Course Fees",
    amount: 25000,
    method: "UPI (GPay/PhonePe)",
    status: "Pending",
    date: "2026-06-20",
    invoiceId: "inv-4"
  }
];

// Invoices
let invoices: Invoice[] = [
  {
    id: "inv-1",
    paymentId: "pay-1",
    invoiceNo: "ALP-2026-0092",
    clientName: "Meenakshi Sundaram",
    clientEmail: "meenakshi.s@yahoo.com",
    itemName: "Premium Couple Match Consultation (ALP method)",
    amount: 2542.37,
    taxAmount: 457.63,
    totalAmount: 3000.0,
    issuedDate: "2026-06-19",
    dueDate: "2026-06-21",
    status: "Paid"
  },
  {
    id: "inv-2",
    paymentId: "pay-2",
    invoiceNo: "ALP-2026-0093",
    clientName: "Balaji Swaminathan",
    clientEmail: "balaji.swami@gmail.com",
    itemName: "ALP Astrology Basic Course Fee",
    amount: 12711.86,
    taxAmount: 2288.14,
    totalAmount: 15000.0,
    issuedDate: "2026-06-15",
    dueDate: "2026-06-15",
    status: "Paid"
  },
  {
    id: "inv-3",
    paymentId: "pay-3",
    invoiceNo: "ALP-2026-0094",
    clientName: "Karthik Raja",
    clientEmail: "karthikr@gmail.com",
    itemName: "ALP Advanced Timing Techniques Fee",
    amount: 21186.44,
    taxAmount: 3813.56,
    totalAmount: 25000.0,
    issuedDate: "2026-05-09",
    dueDate: "2026-05-09",
    status: "Paid"
  },
  {
    id: "inv-4",
    paymentId: "pay-4",
    invoiceNo: "ALP-2026-0095",
    clientName: "Aishwarya Rajesh",
    clientEmail: "aishwarya.rajesh@outlook.com",
    itemName: "ALP Advanced Timing Techniques Fee",
    amount: 21186.44,
    taxAmount: 3813.56,
    totalAmount: 25000.0,
    issuedDate: "2026-06-20",
    dueDate: "2026-06-23",
    status: "Unpaid"
  }
];

// Certificates
let certificates: Certificate[] = [
  {
    id: "cert-1",
    studentId: "stud-2",
    studentName: "Subhashini Sridhar",
    courseName: "ALP Astrology Basic Course",
    certificateNo: "ALP-CRT-98124",
    issueDate: "2026-04-30",
    type: "Course Certificate",
    qrData: `${process.env.APP_URL || "http://localhost:3000"}/verify-credential?id=ALP-CRT-98124`,
    verifiedAt: "2026-05-05T12:00:00Z"
  }
];

// Events
let events: Event[] = [
  {
    id: "ev-1",
    title: "Global Akshaya Lagna Paddhati Workshop 2026",
    description: "An intensive 3-day online workshop on precision timing of marriage blockages, led by Dr. K. Muralidharan.",
    dateTime: "2026-07-15T09:00:00.000Z",
    venueType: "Online",
    venueDetails: "Zoom Broadcast Link (Private Event ID: 914-124-11)",
    ticketPrice: 1500,
    totalSeats: 300,
    seatsBooked: 184
  },
  {
    id: "ev-2",
    title: "Chennai ALP Scholars In-Person Meetup",
    description: "Annual networking and medical astrology showcase for researchers and premium community members in Chennai.",
    dateTime: "2026-08-01T15:00:00.000Z",
    venueType: "Offline",
    venueDetails: "Hall B, Savera Hotels, Dr. Radhakrishnan Salai, Mylapore, Chennai",
    ticketPrice: 500,
    totalSeats: 100,
    seatsBooked: 45
  }
];

// Event Registrations
let eventRegistrations: EventRegistration[] = [
  {
    id: "ereg-1",
    eventId: "ev-1",
    eventTitle: "Global Akshaya Lagna Paddhati Workshop 2026",
    memberName: "Sridhar Kalyanam",
    memberEmail: "skalyanam@gmail.com",
    bookingDate: "2026-06-18",
    ticketCount: 1,
    amountPaid: 1500,
    checkedIn: false
  }
];

// Community Members
let communityMembers: CommunityMember[] = [
  {
    id: "cm-1",
    name: "Dr. Vasudevan Chidambaram",
    email: "vasu.c@madrasuniversity.edu",
    category: "Researcher",
    activeGroup: "Geopolitical Research Wing",
    location: "Chennai, India",
    joinedDate: "2023-01-10"
  },
  {
    id: "cm-2",
    name: "Vandana Sharma",
    email: "sharma.vandana@gmail.com",
    category: "Premium Member",
    activeGroup: "Global VIP Astrology Circle",
    location: "Delhi, India",
    joinedDate: "2024-06-20"
  },
  {
    id: "cm-3",
    name: "Sundar Rajan",
    email: "sraj@volunteers.alpastrology.com",
    category: "Volunteer",
    activeGroup: "Workshops Coordination Committee",
    location: "Madurai, India",
    joinedDate: "2025-02-15"
  }
];

// Social Media Posts
let socialMediaPosts: SocialMediaPost[] = [
  {
    id: "smp-1",
    platform: "YouTube",
    title: "Timing Career Upgrades Using Progressed Akshaya Lagna - Live Session",
    scheduledDate: "2026-06-22",
    status: "Scheduled",
    campaignName: "Q3 Academy Enrollments",
    clicks: 1420,
    leadsGenerated: 34
  },
  {
    id: "smp-2",
    platform: "Instagram",
    title: "Astrological House transits: Venus conjunct Rahu remedies explained in 60s #reels",
    scheduledDate: "2026-06-18",
    status: "Published",
    campaignName: "Consultation Bookings Drive",
    clicks: 840,
    leadsGenerated: 12
  },
  {
    id: "smp-3",
    platform: "Facebook",
    title: "Batch Launch Announcement: Join our Saturday ALP Tamil Basic Course starting July 4th!",
    scheduledDate: "2026-06-21",
    status: "Draft",
    campaignName: "Q3 Academy Enrollments",
    clicks: 0,
    leadsGenerated: 0
  }
];

// Tasks database
let tasks: Task[] = [
  { id: "t-1", title: "Call Srinivasan Raman re: Career consultation matching", assignedTo: "Astrologer Ramanujan", dueDate: "2026-06-20", status: "Pending", priority: "High" },
  { id: "t-2", title: "Verify draft certificate hashes for June graduation cohort", assignedTo: "Dr. K. Muralidharan", dueDate: "2026-06-20", status: "In Progress", priority: "Medium" },
  { id: "t-3", title: "Promote Saturday Tamil Basic Course on WhatsApp Broadcasts", assignedTo: "Social Media Manager", dueDate: "2026-06-21", status: "Pending", priority: "High" }
];

// Audit logs
let auditLogs: AuditLog[] = [
  {
    id: "log-1",
    userId: "u-1",
    userName: "Dr. K. Muralidharan",
    userRole: "Super Admin",
    action: "Lead Status Modified",
    target: "Srinivasan Raman (New Lead -> Contacted)",
    timestamp: "2026-06-20T11:02:15Z"
  },
  {
    id: "log-2",
    userId: "u-1",
    userName: "Dr. K. Muralidharan",
    userRole: "Super Admin",
    action: "Certificate Signed",
    target: "Subhashini Sridhar (ALP-CRT-98124)",
    timestamp: "2026-06-20T11:55:00Z"
  }
];

// Notifications
let notifications: Notification[] = [
  { id: "n-1", title: "New Lead Registered", message: "Vikram Shah registered via Facebook ad campaign.", timestamp: "2026-06-20T04:30:00Z", read: false },
  { id: "n-2", title: "Payment Cleared", message: "Meenakshi Sundaram paid INR 3,000 for Marriage Matching slot.", timestamp: "2026-06-19T15:30:00Z", read: true }
];

// ==========================================
// REST API ROUTER & ENDPOINTS
// ==========================================

// Auth Routes
app.get("/api/auth/me", (req, res) => {
  res.json({ authenticated: true, user: currentUser });
});

app.post("/api/auth/switch-role", (req, res) => {
  const { role } = req.body;
  if (defaultRoles.includes(role)) {
    currentUser = {
      ...currentUser,
      role: role as UserRole,
      name: `Simulated ${role}`
    };
    // Log role switch audit
    const newLog: AuditLog = {
      id: `log-${Date.now()}`,
      userId: currentUser.id,
      userName: currentUser.name,
      userRole: currentUser.role,
      action: "Role Switch",
      target: `Switched view mode to ${role}`,
      timestamp: new Date().toISOString()
    };
    auditLogs.unshift(newLog);
    res.json({ success: true, user: currentUser });
  } else {
    res.status(400).json({ error: "Invalid simulated role" });
  }
});

// Worker Login and Employee Management Routes
app.post("/api/auth/login", (req, res) => {
  const { email, password } = req.body;
  if (!email || !password) {
    return res.status(400).json({ error: "Email and password are required" });
  }

  const employee = employees.find(
    emp => emp.email.toLowerCase().trim() === email.toLowerCase().trim() && emp.password === password
  );

  if (employee) {
    if (employee.status !== "Active") {
      return res.status(403).json({ error: "Employee account is suspended or on leave" });
    }

    currentUser = {
      id: employee.id,
      name: employee.name,
      email: employee.email,
      role: employee.role,
      phone: employee.phone,
      avatar: employee.avatar || `https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=100`
    };

    // Log login audit
    const newLog: AuditLog = {
      id: `log-${Date.now()}`,
      userId: currentUser.id,
      userName: currentUser.name,
      userRole: currentUser.role,
      action: "Login Success",
      target: `Worker logged in successfully: ${currentUser.email}`,
      timestamp: new Date().toISOString()
    };
    auditLogs.unshift(newLog);

    res.json({ success: true, user: currentUser });
  } else {
    res.status(401).json({ error: "Invalid email or password credentials" });
  }
});

app.post("/api/auth/logout", (req, res) => {
  const oldUser = currentUser;
  currentUser = {
    id: "guest",
    name: "Guest User",
    email: "guest@alp.org",
    role: "Super Admin",
    phone: "",
    avatar: ""
  };

  const newLog: AuditLog = {
    id: `log-${Date.now()}`,
    userId: oldUser.id,
    userName: oldUser.name,
    userRole: oldUser.role,
    action: "Logout Success",
    target: `Worker logged out successfully: ${oldUser.email}`,
    timestamp: new Date().toISOString()
  };
  auditLogs.unshift(newLog);

  res.json({ success: true, user: null });
});

app.get("/api/employees", (req, res) => {
  res.json(employees);
});

app.post("/api/employees", (req, res) => {
  const { name, email, password, role, phone, avatar, status } = req.body;

  if (!name || !email || !password || !role) {
    return res.status(400).json({ error: "Name, email, password, and role are required parameters." });
  }

  // Check if email already registered
  const exists = employees.some(emp => emp.email.toLowerCase().trim() === email.toLowerCase().trim());
  if (exists) {
    return res.status(400).json({ error: "An employee with this email already exists." });
  }

  const newEmp: Employee = {
    id: `emp-${Date.now()}`,
    name,
    email: email.trim(),
    password,
    role,
    phone: phone || "",
    avatar: avatar || `https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=100`,
    createdTime: new Date().toISOString(),
    status: status || "Active"
  };

  employees.push(newEmp);

  // Log audit
  const newLog: AuditLog = {
    id: `log-${Date.now()}`,
    userId: currentUser.id,
    userName: currentUser.name,
    userRole: currentUser.role,
    action: "Create Employee",
    target: `Registered co-worker: ${name} (${role})`,
    timestamp: new Date().toISOString()
  };
  auditLogs.unshift(newLog);

  res.status(201).json(newEmp);
});

app.put("/api/employees/:id", (req, res) => {
  const { id } = req.params;
  const { name, email, password, role, phone, avatar, status } = req.body;

  const employeeIndex = employees.findIndex(emp => emp.id === id);
  if (employeeIndex === -1) {
    return res.status(404).json({ error: "Employee record not found" });
  }

  const currentEmp = employees[employeeIndex];

  // Update fields
  if (name) currentEmp.name = name;
  if (email) currentEmp.email = email;
  if (password) currentEmp.password = password;
  if (role) currentEmp.role = role;
  if (phone !== undefined) currentEmp.phone = phone;
  if (avatar) currentEmp.avatar = avatar;
  if (status) currentEmp.status = status;

  // Log audit
  const newLog: AuditLog = {
    id: `log-${Date.now()}`,
    userId: currentUser.id,
    userName: currentUser.name,
    userRole: currentUser.role,
    action: "Update Employee",
    target: `Modified co-worker record: ${currentEmp.name} (${currentEmp.id})`,
    timestamp: new Date().toISOString()
  };
  auditLogs.unshift(newLog);

  res.json(currentEmp);
});

app.delete("/api/employees/:id", (req, res) => {
  const { id } = req.params;
  
  if (id === "emp-1" || id === currentUser.id) {
    return res.status(400).json({ error: "Cannot delete the root founder or yourself" });
  }

  const employeeIndex = employees.findIndex(emp => emp.id === id);
  if (employeeIndex === -1) {
    return res.status(404).json({ error: "Employee record not found" });
  }

  const removed = employees[employeeIndex];
  employees.splice(employeeIndex, 1);

  // Log audit
  const newLog: AuditLog = {
    id: `log-${Date.now()}`,
    userId: currentUser.id,
    userName: currentUser.name,
    userRole: currentUser.role,
    action: "Delete Employee",
    target: `Removed co-worker: ${removed.name} (${removed.email})`,
    timestamp: new Date().toISOString()
  };
  auditLogs.unshift(newLog);

  res.json({ success: true, message: "Employee removed successfully" });
});

// Leads Router
app.get("/api/leads", (req, res) => {
  res.json(leads);
});

app.post("/api/leads/import", (req, res) => {
  const { leads: importLeads } = req.body;
  if (!importLeads || !Array.isArray(importLeads)) {
    return res.status(400).json({ error: "Invalid payload, must provide a list of leads" });
  }

  const importedList: Lead[] = [];
  for (const item of importLeads) {
    const { name, mobile, email, country, city, language, source, interestType, notes, assignedStaff } = item;
    if (!name || !mobile) {
      continue;
    }

    const newLead: Lead = {
      id: `lead-${Date.now()}-${Math.random().toString(36).substring(2, 7)}`,
      name: String(name).trim(),
      mobile: String(mobile).trim(),
      email: email ? String(email).trim() : "",
      country: country ? String(country).trim() : "India",
      city: city ? String(city).trim() : "",
      language: language ? String(language).trim() : "Tamil",
      source: (source ? String(source).trim() : "Manual Entry") as any,
      interestType: (interestType ? String(interestType).trim() : "Consultation") as any,
      status: "New Lead",
      notes: notes ? String(notes).trim() : "",
      assignedStaff: assignedStaff ? String(assignedStaff).trim() : "Unassigned",
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString()
    };

    leads.unshift(newLead);
    importedList.push(newLead);

    // Auto-log initial timeline registration
    const newActivity: LeadActivity = {
      id: `act-${Date.now()}-${Math.random().toString(36).substring(2, 7)}`,
      leadId: newLead.id,
      staffName: "System Importer",
      actionType: "Note Added",
      details: "Lead imported successfully via Excel format upload.",
      timestamp: new Date().toISOString()
    };
    leadActivities.push(newActivity);
  }

  // Auditing bulk import
  if (importedList.length > 0) {
    const audit: AuditLog = {
      id: `log-${Date.now()}`,
      userId: currentUser.id,
      userName: currentUser.name,
      userRole: currentUser.role,
      action: "Lead Bulk Import",
      target: `${importedList.length} leads imported via Excel/CSV file`,
      timestamp: new Date().toISOString()
    };
    auditLogs.unshift(audit);
  }

  res.json({ success: true, imported: importedList });
});

app.post("/api/leads", (req, res) => {
  const { name, mobile, email, country, city, language, source, interestType, notes, assignedStaff } = req.body;
  
  if (!name || !mobile) {
    return res.status(400).json({ error: "Name and Mobile are required Fields" });
  }

  const newLead: Lead = {
    id: `lead-${Date.now()}`,
    name,
    mobile,
    email: email || "",
    country: country || "India",
    city: city || "",
    language: language || "Tamil",
    source: source || "Manual Entry",
    interestType: interestType || "Consultation",
    status: "New Lead",
    notes: notes || "",
    assignedStaff: assignedStaff || "Unassigned",
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString()
  };

  leads.unshift(newLead);

  // Auto-log initial timeline registration
  const newActivity: LeadActivity = {
    id: `act-${Date.now()}`,
    leadId: newLead.id,
    staffName: currentUser.name,
    actionType: "Note Added",
    details: `Lead qualified and manually registered in CRM pipeline. Interested in ${interestType}.`,
    timestamp: new Date().toISOString()
  };
  leadActivities.push(newActivity);

  // Audit list
  const audit: AuditLog = {
    id: `log-${Date.now()}`,
    userId: currentUser.id,
    userName: currentUser.name,
    userRole: currentUser.role,
    action: "Lead Created",
    target: `${name} (${newLead.source})`,
    timestamp: new Date().toISOString()
  };
  auditLogs.unshift(audit);

  res.json(newLead);
});

app.put("/api/leads/:id", (req, res) => {
  const { id } = req.params;
  const leadIndex = leads.findIndex(l => l.id === id);

  if (leadIndex === -1) {
    return res.status(444).json({ error: "Lead not found" });
  }

  const currentLead = leads[leadIndex];
  const updatedFields = req.body;

  // Track status transition for timeline logs
  if (updatedFields.status && updatedFields.status !== currentLead.status) {
    const newActivity: LeadActivity = {
      id: `act-${Date.now()}`,
      leadId: id,
      staffName: currentUser.name,
      actionType: "Status Change",
      details: `Funnel pipeline shifted from of [${currentLead.status}] to [${updatedFields.status}]`,
      timestamp: new Date().toISOString()
    };
    leadActivities.push(newActivity);
  }

  const updatedLead = {
    ...currentLead,
    ...updatedFields,
    updatedAt: new Date().toISOString()
  };

  leads[leadIndex] = updatedLead;

  // Audit update
  const audit: AuditLog = {
    id: `log-${Date.now()}`,
    userId: currentUser.id,
    userName: currentUser.name,
    userRole: currentUser.role,
    action: "Lead Fields Modified",
    target: `${updatedLead.name} edits logged.`,
    timestamp: new Date().toISOString()
  };
  auditLogs.unshift(audit);

  res.json(updatedLead);
});

// Bulk assign leads to coworkers (traditional CRM distribution)
app.put("/api/leads/bulk/assign", (req, res) => {
  const { assignments } = req.body;
  if (!assignments || !Array.isArray(assignments)) {
    return res.status(400).json({ error: "Assignments list is required" });
  }

  const updatedList: any[] = [];
  assignments.forEach(({ leadId, assignedStaff }) => {
    const idx = leads.findIndex(l => l.id === leadId);
    if (idx !== -1) {
      leads[idx] = {
        ...leads[idx],
        assignedStaff: assignedStaff || "Unassigned",
        updatedAt: new Date().toISOString()
      };
      updatedList.push(leads[idx]);

      // Log assignment activity
      const newActivity: LeadActivity = {
        id: `act-${Date.now()}-${Math.random().toString(36).substring(2, 6)}`,
        leadId,
        staffName: currentUser.name,
        actionType: "Note Added",
        details: `Assigned to CRM advisor [${assignedStaff}]`,
        timestamp: new Date().toISOString()
      };
      leadActivities.push(newActivity);
    }
  });

  const audit: AuditLog = {
    id: `log-${Date.now()}`,
    userId: currentUser.id,
    userName: currentUser.name,
    userRole: currentUser.role,
    action: "Bulk Lead Separation Distribution",
    target: `${assignments.length} leads distributed to coworkers`,
    timestamp: new Date().toISOString()
  };
  auditLogs.unshift(audit);

  res.json({ success: true, updatedLeads: updatedList });
});

// Fetch Timeline activity for a single lead
app.get("/api/leads/:id/activity", (req, res) => {
  const { id } = req.params;
  const list = leadActivities.filter(a => a.leadId === id);
  res.json(list);
});

app.post("/api/leads/:id/activity", (req, res) => {
  const { id } = req.params;
  const { actionType, details } = req.body;

  if (!actionType || !details) {
    return res.status(400).json({ error: "ActionType and Details are required parameters" });
  }

  const act: LeadActivity = {
    id: `act-${Date.now()}`,
    leadId: id,
    staffName: currentUser.name,
    actionType,
    details,
    timestamp: new Date().toISOString()
  };

  leadActivities.push(act);
  res.json(act);
});

// Clients & Consultations
app.get("/api/clients", (req, res) => {
  res.json(clients);
});

app.post("/api/clients", (req, res) => {
  const newClient: Client = {
    id: `client-${Date.now()}`,
    ...req.body
  };
  clients.unshift(newClient);
  res.json(newClient);
});

app.get("/api/consultations", (req, res) => {
  res.json(consultations);
});

app.post("/api/consultations", (req, res) => {
  const { clientId, clientName, type, dateTime, astrologerId, astrologerName, meetingLink } = req.body;
  if (!clientId || !dateTime) {
    return res.status(400).json({ error: "Missing required booking details (ClientId & DateTime)" });
  }

  const newBooking: Consultation = {
    id: `cons-${Date.now()}`,
    clientId,
    clientName: clientName || clients.find(c => c.id === clientId)?.name || "Unknown client",
    type: type || "General Guidance",
    dateTime,
    status: "Scheduled",
    astrologerId: astrologerId || "astro-muralidharan",
    astrologerName: astrologerName || "Dr. K. Muralidharan",
    meetingLink: meetingLink || "https://meet.google.com/mvw-wsjc-alp"
  };

  consultations.unshift(newBooking);

  // Log audit
  const audit: AuditLog = {
    id: `log-${Date.now()}`,
    userId: currentUser.id,
    userName: currentUser.name,
    userRole: currentUser.role,
    action: "Consultation Booked",
    target: `${newBooking.clientName} booked with ${newBooking.astrologerName}`,
    timestamp: new Date().toISOString()
  };
  auditLogs.unshift(audit);

  res.json(newBooking);
});

// Complete Consultation status or upload recording link
app.put("/api/consultations/:id", (req, res) => {
  const { id } = req.params;
  const idx = consultations.findIndex(c => c.id === id);
  if (idx === -1) return res.status(404).json({ error: "Appointment not found" });

  consultations[idx] = {
    ...consultations[idx],
    ...req.body
  };
  res.json(consultations[idx]);
});

// Consultation notes
app.get("/api/consultations/:id/notes", (req, res) => {
  const list = consultationNotes.filter(n => n.consultationId === req.params.id);
  res.json(list);
});

app.post("/api/consultations/:id/notes", (req, res) => {
  const { id } = req.params;
  const { title, content } = req.body;

  const newNote: ConsultationNote = {
    id: `note-${Date.now()}`,
    consultationId: id,
    writerName: currentUser.name,
    title: title || "Progress Notes",
    content,
    timestamp: new Date().toISOString()
  };

  consultationNotes.push(newNote);
  res.json(newNote);
});

// Courses, batches and Academy
app.get("/api/courses", (req, res) => {
  res.json(courses);
});

app.get("/api/batches", (req, res) => {
  res.json(batches);
});

app.get("/api/students", (req, res) => {
  res.json(students);
});

app.get("/api/student-enrollments", (req, res) => {
  res.json(studentEnrollments);
});

app.get("/api/enrollments", (req, res) => {
  res.json(studentEnrollments);
});

app.post("/api/students", (req, res) => {
  const newStudent: Student = {
    id: `stud-${Date.now()}`,
    name: req.body.name,
    phone: req.body.phone,
    email: req.body.email,
    country: req.body.country || "India",
    occupation: req.body.occupation || "N/A",
    joinedDate: new Date().toISOString().split("T")[0]
  };

  students.unshift(newStudent);
  res.json(newStudent);
});

app.post("/api/students/enroll", (req, res) => {
  const { studentId, courseId, batchId } = req.body;
  const stud = students.find(s => s.id === studentId);
  const crs = courses.find(c => c.id === courseId);
  const bat = batches.find(b => b.id === batchId);

  if (!stud || !crs || !bat) {
    return res.status(400).json({ error: "Invalid Student, Course or Batch payload references" });
  }

  const enr: StudentEnrollment = {
    id: `enr-${Date.now()}`,
    studentId,
    studentName: stud.name,
    courseId,
    courseName: crs.name,
    batchId,
    batchName: bat.name,
    enrollmentDate: new Date().toISOString().split("T")[0],
    progressPercentage: 0,
    attendancePercentage: 100,
    status: "In Progress"
  };

  studentEnrollments.unshift(enr);

  // Auto-generate unpaid course fee invoice or payment pending
  const payId = `pay-${Date.now()}`;
  const invId = `inv-${Date.now()}`;

  const pendingPay: Payment = {
    id: payId,
    payerName: stud.name,
    payerEmail: stud.email,
    type: "Course Fees",
    amount: crs.fees,
    method: "Online (Stripe)",
    status: "Pending",
    date: new Date().toISOString().split("T")[0],
    invoiceId: invId
  };
  payments.unshift(pendingPay);

  const pendingInv: Invoice = {
    id: invId,
    paymentId: payId,
    invoiceNo: `ALP-2026-${Math.floor(1000 + Math.random() * 9000)}`,
    clientName: stud.name,
    clientEmail: stud.email,
    itemName: `${crs.name} Academics Enrollment Fee`,
    amount: Number((crs.fees / 1.18).toFixed(2)),
    taxAmount: Number((crs.fees - crs.fees / 1.18).toFixed(2)),
    totalAmount: crs.fees,
    issuedDate: new Date().toISOString().split("T")[0],
    dueDate: new Date(Date.now() + 3 * 24 * 60 * 60 * 1000).toISOString().split("T")[0],
    status: "Unpaid"
  };
  invoices.unshift(pendingInv);

  res.json(enr);
});

// Update enrollment metrics or graduate student
app.put("/api/student-enrollments/:id", (req, res) => {
  const { id } = req.params;
  const idx = studentEnrollments.findIndex(e => e.id === id);
  if (idx === -1) return res.status(404).json({ error: "Enrollment not found" });

  studentEnrollments[idx] = {
    ...studentEnrollments[idx],
    ...req.body
  };
  res.json(studentEnrollments[idx]);
});

app.put("/api/enrollments/:id", (req, res) => {
  const { id } = req.params;
  const idx = studentEnrollments.findIndex(e => e.id === id);
  if (idx === -1) return res.status(404).json({ error: "Enrollment not found" });

  studentEnrollments[idx] = {
    ...studentEnrollments[idx],
    ...req.body
  };
  res.json(studentEnrollments[idx]);
});

app.post("/api/enrollments", (req, res) => {
  const { studentId, courseId, batchId } = req.body;
  const stud = students.find(s => s.id === studentId);
  const crs = courses.find(c => c.id === courseId);
  const bat = batches.find(b => b.id === batchId);

  if (!stud || !crs || !bat) {
    return res.status(400).json({ error: "Invalid Student, Course or Batch payload references" });
  }

  const enr: StudentEnrollment = {
    id: `enr-${Date.now()}`,
    studentId,
    studentName: stud.name,
    courseId,
    courseName: crs.name,
    batchId,
    batchName: bat.name,
    enrollmentDate: new Date().toISOString().split("T")[0],
    progressPercentage: 0,
    attendancePercentage: 100,
    status: "In Progress"
  };

  studentEnrollments.unshift(enr);

  // Auto-generate unpaid course fee invoice or payment pending
  const payId = `pay-${Date.now()}`;
  const invId = `inv-${Date.now()}`;

  const pendingPay: Payment = {
    id: payId,
    payerName: stud.name,
    payerEmail: stud.email,
    type: "Course Fees",
    amount: crs.fees,
    method: "Bank Transfer",
    status: "Pending",
    date: new Date().toISOString().split("T")[0]
  };
  payments.unshift(pendingPay);

  const pendingInv: Invoice = {
    id: invId,
    paymentId: payId,
    invoiceNo: `INV-${Date.now().toString().slice(-6)}`,
    clientName: stud.name,
    clientEmail: stud.email,
    itemName: `${crs.name} Academics Enrollment Fee`,
    amount: Number((crs.fees / 1.18).toFixed(2)),
    taxAmount: Number((crs.fees - crs.fees / 1.18).toFixed(2)),
    totalAmount: crs.fees,
    issuedDate: new Date().toISOString().split("T")[0],
    dueDate: new Date(Date.now() + 3 * 24 * 60 * 60 * 1000).toISOString().split("T")[0],
    status: "Unpaid"
  };
  invoices.unshift(pendingInv);

  res.json(enr);
});

// Financial Ledger
app.get("/api/payments", (req, res) => {
  res.json(payments);
});

app.post("/api/payments", (req, res) => {
  const { payerName, payerEmail, type, amount, method, status } = req.body;
  const payId = `pay-${Date.now()}`;
  const invId = `inv-${Date.now()}`;

  const newPay: Payment = {
    id: payId,
    payerName,
    payerEmail,
    type: type || "Consultation Fees",
    amount: Number(amount),
    method: method || "UPI (GPay/PhonePe)",
    status: status || "Paid",
    date: new Date().toISOString().split("T")[0],
    invoiceId: invId
  };

  const newInv: Invoice = {
    id: invId,
    paymentId: payId,
    invoiceNo: `ALP-2026-${Math.floor(1000 + Math.random() * 9000)}`,
    clientName: payerName,
    clientEmail: payerEmail,
    itemName: `${type} Ledger Booking Ref`,
    amount: Number((amount / 1.18).toFixed(2)),
    taxAmount: Number((amount - amount / 1.18).toFixed(2)),
    totalAmount: Number(amount),
    issuedDate: new Date().toISOString().split("T")[0],
    dueDate: new Date().toISOString().split("T")[0],
    status: status === "Paid" ? "Paid" : "Unpaid"
  };

  payments.unshift(newPay);
  invoices.unshift(newInv);

  // Check if paid, audit log
  if (newPay.status === "Paid") {
    const audit: AuditLog = {
      id: `log-${Date.now()}`,
      userId: currentUser.id,
      userName: currentUser.name,
      userRole: currentUser.role,
      action: "Payment Realized",
      target: `Received INR ${amount} from ${payerName}`,
      timestamp: new Date().toISOString()
    };
    auditLogs.unshift(audit);
  }

  res.json(newPay);
});

app.get("/api/invoices", (req, res) => {
  res.json(invoices);
});

// Approve Pending Payment (Manual check realization)
app.put("/api/payments/:id/clear", (req, res) => {
  const pIdx = payments.findIndex(p => p.id === req.params.id);
  if (pIdx !== -1) {
    payments[pIdx].status = "Paid";
    
    // Clear invoice also
    const invId = payments[pIdx].invoiceId;
    const iIdx = invoices.findIndex(i => i.id === invId);
    if (iIdx !== -1) {
      invoices[iIdx].status = "Paid";
    }

    const audit: AuditLog = {
      id: `log-${Date.now()}`,
      userId: currentUser.id,
      userName: currentUser.name,
      userRole: currentUser.role,
      action: "Payment Verified",
      target: `Cleared balance of INR ${payments[pIdx].amount} for ${payments[pIdx].payerName}`,
      timestamp: new Date().toISOString()
    };
    auditLogs.unshift(audit);
    
    return res.json({ success: true, payment: payments[pIdx] });
  }
  res.status(404).json({ error: "Transaction not found" });
});

// Academy Certificates Router
app.get("/api/certificates", (req, res) => {
  res.json(certificates);
});

app.post("/api/certificates", (req, res) => {
  const { studentId, studentName, courseName, type } = req.body;
  if (!studentId || !studentName || !courseName) {
    return res.status(400).json({ error: "Missing print parameters (studentId, studentName, courseName)" });
  }

  const certNo = `ALP-CRT-${Math.floor(10000 + Math.random() * 89999)}`;
  const valUrl = `${process.env.APP_URL || "http://localhost:3000"}/verify-credential?id=${certNo}`;

  const cert: Certificate = {
    id: `cert-${Date.now()}`,
    studentId,
    studentName,
    courseName,
    certificateNo: certNo,
    issueDate: new Date().toISOString().split("T")[0],
    type: type || "Course Certificate",
    qrData: valUrl
  };

  certificates.unshift(cert);

  // Update original enrollments status as graduated
  const eIdx = studentEnrollments.findIndex(e => e.studentId === studentId && e.courseName === courseName);
  if (eIdx !== -1) {
    studentEnrollments[eIdx].status = "Completed";
    studentEnrollments[eIdx].progressPercentage = 100;
  }

  // Audit
  const audit: AuditLog = {
    id: `log-${Date.now()}`,
    userId: currentUser.id,
    userName: currentUser.name,
    userRole: currentUser.role,
    action: "Certificate Signed & Published",
    target: `${studentName} - Serial ${certNo}`,
    timestamp: new Date().toISOString()
  };
  auditLogs.unshift(audit);

  res.json(cert);
});

// Public verify certification link
app.get("/api/certificates/verify/:no", (req, res) => {
  const { no } = req.params;
  const cert = certificates.find(c => c.certificateNo === no);
  if (cert) {
    res.json({ verified: true, certificate: cert });
  } else {
    res.json({ verified: false, message: "Valid Certificate reference is not indexed inside global ledger." });
  }
});

// Community Members
app.get("/api/community", (req, res) => {
  res.json(communityMembers);
});

app.get("/api/community/members", (req, res) => {
  res.json(communityMembers);
});

app.post("/api/community", (req, res) => {
  const { name, email, category, activeGroup, location } = req.body;
  const newM: CommunityMember = {
    id: `cm-${Date.now()}`,
    name,
    email,
    category: category || "Premium Members",
    activeGroup: activeGroup || "General Forum",
    location: location || "Global",
    joinedDate: new Date().toISOString().split("T")[0]
  };
  communityMembers.unshift(newM);
  res.json(newM);
});

app.post("/api/community/members", (req, res) => {
  const { name, email, category, activeGroup, location } = req.body;
  const newM: CommunityMember = {
    id: `cm-${Date.now()}`,
    name,
    email,
    category: category || "Premium Members",
    activeGroup: activeGroup || "General Forum",
    location: location || "Global",
    joinedDate: new Date().toISOString().split("T")[0]
  };
  communityMembers.unshift(newM);
  res.json(newM);
});

// Events & Registrations
app.get("/api/events", (req, res) => {
  res.json(events);
});

app.post("/api/events", (req, res) => {
  const { title, description, dateTime, venueType, venueDetails, ticketPrice, totalSeats } = req.body;
  const ev: Event = {
    id: `ev-${Date.now()}`,
    title,
    description: description || "",
    dateTime,
    venueType: venueType || "Online",
    venueDetails: venueDetails || "Zoom Broadcast",
    ticketPrice: Number(ticketPrice || 0),
    totalSeats: Number(totalSeats || 100),
    seatsBooked: 0
  };
  events.unshift(ev);
  res.json(ev);
});

app.post("/api/events/:id/register", (req, res) => {
  const { id } = req.params;
  const { memberName, memberEmail, ticketCount } = req.body;
  const evIdx = events.findIndex(e => e.id === id);

  if (evIdx === -1) return res.status(404).json({ error: "Target Event not active" });
  const eventObj = events[evIdx];

  const tickets = Number(ticketCount || 1);
  if (eventObj.seatsBooked + tickets > eventObj.totalSeats) {
    return res.status(400).json({ error: "Sold out! Seats limit bounds exceeded" });
  }

  const amt = eventObj.ticketPrice * tickets;

  const reg: EventRegistration = {
    id: `ereg-${Date.now()}`,
    eventId: id,
    eventTitle: eventObj.title,
    memberName,
    memberEmail,
    bookingDate: new Date().toISOString().split("T")[0],
    ticketCount: tickets,
    amountPaid: amt,
    checkedIn: false
  };

  events[evIdx].seatsBooked += tickets;
  eventRegistrations.unshift(reg);

  // Auto payment log if ticket price > 0
  if (amt > 0) {
    const payId = `pay-${Date.now()}`;
    payments.unshift({
      id: payId,
      payerName: memberName,
      payerEmail: memberEmail,
      type: "Events",
      amount: amt,
      method: "Online (Stripe)",
      status: "Paid",
      date: new Date().toISOString().split("T")[0]
    });
  }

  res.json(reg);
});

app.post("/api/events/:id/book-ticket", (req, res) => {
  const { id } = req.params;
  const { memberName, memberEmail, ticketCount } = req.body;
  const evIdx = events.findIndex(e => e.id === id);

  if (evIdx === -1) return res.status(404).json({ error: "Target Event not active" });
  const eventObj = events[evIdx];

  const tickets = Number(ticketCount || 1);
  if (eventObj.seatsBooked + tickets > eventObj.totalSeats) {
    return res.status(400).json({ error: "Sold out! Seats limit bounds exceeded" });
  }

  const amt = eventObj.ticketPrice * tickets;

  const reg: EventRegistration = {
    id: `ereg-${Date.now()}`,
    eventId: id,
    eventTitle: eventObj.title,
    memberName,
    memberEmail,
    bookingDate: new Date().toISOString().split("T")[0],
    ticketCount: tickets,
    amountPaid: amt,
    checkedIn: false
  };

  events[evIdx].seatsBooked += tickets;
  eventRegistrations.unshift(reg);

  // Auto payment log if ticket price > 0
  if (amt > 0) {
    const payId = `pay-${Date.now()}`;
    payments.unshift({
      id: payId,
      payerName: memberName,
      payerEmail: memberEmail,
      type: "Events",
      amount: amt,
      method: "Online (Stripe)",
      status: "Paid",
      date: new Date().toISOString().split("T")[0]
    });
  }

  res.json(eventObj);
});

// Social Posts Campaign Tracker
app.get("/api/social", (req, res) => {
  res.json(socialMediaPosts);
});

app.get("/api/social/posts", (req, res) => {
  res.json(socialMediaPosts);
});

app.post("/api/social", (req, res) => {
  const { platform, title, scheduledDate, campaignName } = req.body;
  const newPost: SocialMediaPost = {
    id: `smp-${Date.now()}`,
    platform: platform || "YouTube",
    title,
    scheduledDate: scheduledDate || new Date().toISOString().split("T")[0],
    status: "Scheduled",
    campaignName: campaignName || "N/A",
    clicks: 0,
    leadsGenerated: 0
  };
  socialMediaPosts.unshift(newPost);
  res.json(newPost);
});

app.post("/api/social/posts", (req, res) => {
  const { platform, title, scheduledDate, campaignName } = req.body;
  const newPost: SocialMediaPost = {
    id: `smp-${Date.now()}`,
    platform: platform || "YouTube",
    title,
    scheduledDate: scheduledDate || new Date().toISOString().split("T")[0],
    status: "Scheduled",
    campaignName: campaignName || "N/A",
    clicks: 0,
    leadsGenerated: 0
  };
  socialMediaPosts.unshift(newPost);
  res.json(newPost);
});

// Tasks
app.get("/api/tasks", (req, res) => {
  res.json(tasks);
});

app.post("/api/tasks", (req, res) => {
  const { title, assignedTo, dueDate, priority, leadId, isReminder, reminderTime } = req.body;
  const t: Task = {
    id: `t-${Date.now()}`,
    title,
    assignedTo: assignedTo || currentUser.name,
    dueDate: dueDate || new Date().toISOString().split("T")[0],
    status: "Pending",
    priority: priority || "Medium",
    leadId,
    isReminder: !!isReminder,
    reminderTime
  };
  tasks.unshift(t);
  res.json(t);
});

app.put("/api/tasks/:id", (req, res) => {
  const { id } = req.params;
  const idx = tasks.findIndex(t => t.id === id);
  if (idx !== -1) {
    tasks[idx] = {
      ...tasks[idx],
      ...req.body
    };
    return res.json(tasks[idx]);
  }
  res.status(404).json({ error: "Task not found" });
});

// System logs Audit
app.get("/api/audit-logs", (req, res) => {
  res.json(auditLogs);
});

// Get system notification alerts
app.get("/api/notifications", (req, res) => {
  res.json(notifications);
});

app.post("/api/notifications/read", (req, res) => {
  notifications = notifications.map(n => ({ ...n, read: true }));
  res.json({ success: true });
});

app.post("/api/notifications/clear", (req, res) => {
  notifications = notifications.map(n => ({ ...n, read: true }));
  res.json({ success: true });
});

app.post("/api/notifications/log", (req, res) => {
  const { channel, message } = req.body;
  const newNotif = {
    id: `n-${Date.now()}`,
    title: channel || "System EventLog",
    message: message || "General system audit verified.",
    timestamp: new Date().toISOString(),
    read: false
  };
  notifications.unshift(newNotif);
  res.json(newNotif);
});

// ==========================================
// GEMINI INTELLIGENT EXPERT ENGINE
// ==========================================

let aiClient: GoogleGenAI | null = null;

// Secure Lazy initialization for Gemini Client
function getAIClient(): GoogleGenAI {
  if (!aiClient) {
    const key = process.env.GEMINI_API_KEY;
    if (!key || key === "MY_GEMINI_API_KEY") {
      throw new Error("Missing GEMINI_API_KEY in environment variables");
    }
    aiClient = new GoogleGenAI({ apiKey: key });
  }
  return aiClient;
}

// REST route routing to Gemini AI
app.post("/api/ai/analyze-chart", async (req, res) => {
  const { clientName, dob, birthTime, birthPlace, consultationType, notes } = req.body;
  
  if (!clientName || !dob || !birthTime || !birthPlace) {
    return res.status(400).json({ error: "Please input client's birth details: Name, DOB, Time and Place." });
  }

  // Calculate simulated Akshaya Lagna progressed status
  // 1 year equals 1 degree in Akshaya Lagna Paddhati (ALP)
  // Let's compute actual age
  const birthYear = new Date(dob).getFullYear();
  const currentYear = new Date().getFullYear();
  const rawAge = currentYear - birthYear;
  const age = Math.max(0, isNaN(rawAge) ? 30 : rawAge);
  
  // ALP Rule: Progressed Lagna is calculated by shifting 1 degree per year from natal ascendent.
  // There are 30 degrees per zodiac sign (Rasi)
  // Since we don't do full ephemeris calculations client-side, we approximate:
  // e.g. Scorpio birth lagna, with 30 years age shifts the progressed Akshaya Lagna to the next sign, Sagittarius.
  const signs = [
    "Aries (Mesha)", "Taurus (Rishaba)", "Gemini (Mithuna)", "Cancer (Kataka)",
    "Leo (Simha)", "Virgo (Kanya)", "Libra (Tula)", "Scorpio (Viruchika)",
    "Sagittarius (Dhanus)", "Capricorn (Makara)", "Aquarius (Kumbha)", "Pisces (Meena)"
  ];
  const birthHouseIndex = (Math.abs(clientName.charCodeAt(0) + birthPlace.charCodeAt(0))) % 12;
  const natalLagna = signs[birthHouseIndex];
  
  // progress index shifted by degrees (1 age = 1 degree)
  // total zodiac degrees is 360. 30 degrees = 1 sign
  const offsetDegree = age % 30;
  const houseOffset = Math.floor(age / 30) % 12;
  const progressedLagnaIndex = (birthHouseIndex + houseOffset) % 12;
  const progressedLagna = signs[progressedLagnaIndex];

  try {
    const aiInstance = getAIClient();
    
    // Formulate a structured prompt focusing on Akshaya Lagna Paddhati guidelines
    const prompt = `You are a legendary Senior Astrologer expert in Akshaya Lagna Paddhati (ALP) Astrology. 
    A client named ${clientName} requires an analytical timing prediction for ${consultationType || "General Guidance"}.
    
    Birth Details:
    - Date of Birth: ${dob}
    - Time of Birth: ${birthTime}
    - Place of Birth: ${birthPlace}
    - Calculated Age today: ${age} years old
    - Estimated Natal Lagna (Ascendent): ${natalLagna}
    - ALP Progressed Lagna (timing sign shifting 1 degree per year): ${progressedLagna} (Age: ${age} -> shifted ${houseOffset} houses + ${offsetDegree} degrees into ${progressedLagna})
    - Contextual consult notes: ${notes || "General status analysis required."}
    
    Please write a structured, highly scholarly astrological analysis including:
    1. Natal Lagna & Lord placement character profile
    2. Progressed ALP Lagna Analysis: How the current zodiac house (${progressedLagna}) rules current planetary transits. Determine marriage timing (look at Venus/Jupiter transiting 7th from progressed lagna) or career timing (look at 10th house from progressed lagna).
    3. Practical Remedial Advice (specific chants, lightings, and specific days to observe fasts).
    
    Keep the tone extremely professional, wise, traditional, and encouraging. Present output in clean markdown.`;

    const response = await aiInstance.models.generateContent({
      model: "gemini-2.5-flash",
      contents: prompt,
    });

    const aiText = response.text || "Report creation returned empty.";
    res.json({
      success: true,
      age,
      natalLagna,
      progressedLagna,
      progressedDegree: offsetDegree,
      reportText: aiText
    });

  } catch (error: any) {
    console.warn("Gemini API missing or failed, using high-fidelity local ALP Astrology Engine simulation.", error.message);
    
    // Settle beautiful, rich fallback simulator which represents a true bespoke system
    const simulatedAstroReading = `### 🌟 Akshaya Lagna Paddhati (ALP) Scientific Astro Analysis

**Client Name:** ${clientName}
**Current Age:** ${age} Years | **Natal Ascendent (Lagna):** ${natalLagna}
**Current ALP Progressed Lagna:** ${progressedLagna} (at ${offsetDegree}° Position)
**Consultation Focus:** ${consultationType || "General Timing Matrix"}

---

#### 1. Foundational Ascendent & Planetary Configurations
* **Natal Ascendent Profile:** Your natal root houses align with the energies of **${natalLagna}**. This implies that your innate intelligence is governed by its ruling planet, establishing a baseline blueprint focusing on analytical vigor and structured progression.
* **Structural House Alignments:** For ALP astrology, natal placements indicate potential, but current actualized materializations are governed strictly by the **progressed ALP lagna coordinate**.

#### 2. Progressed ALP Lagna Analysis & Timing Matrix
* **ALP Motion Calculation:** In Akshaya Lagna Paddhati, the ascendant progresses physically by **1° per year**. At age **${age}**, your lagna point has completed its orbital shift, settling at **${offsetDegree}°** inside **${progressedLagna}**.
* **Current Operational Cycle:** With the matured progressed lagna acting as your active ascendant, the 10th house representing Career shifts directly into the constellation of **${signs[(progressedLagnaIndex + 9) % 12]}**. 
  * *Career Indicator:* As Saturn or Jupiter transits key cardinal houses relative to your ALP progressor, a significant timing trigger window is active.
  * *Relationship Timing:* The 7th house relative to current ALP progressed lagna falls within **${signs[(progressedLagnaIndex + 6) % 12]}**. Favorable Venus aspects here denote high coordination for marriage contracts and successful unions during this year cycle.

#### 3. Structured Remedial Action Plans (Vastu & Astro Remediation)
* **Energy Grounding:** Light a sesame oil lamp facing East on Saturday mornings to ground any Saturn delay energies.
* **Syllabic Remediation:** Chant the sacred root mantra matching the lord of **${progressedLagna}** 108 times daily to remove professional obstacles.
* **Charitable Timing:** On days of active moon constellations matching your birth nakshatra, distribute yellow lentils or feed birds to boost clean mercury and Venus operational transits.
`;
    res.json({
      success: true,
      age,
      natalLagna,
      progressedLagna,
      progressedDegree: offsetDegree,
      reportText: simulatedAstroReading,
      isSimulated: true
    });
  }
});

// ==========================================
// PORT 3000 DEV SERVER MIDDLEWARE & INGRESS
// ==========================================

async function start() {
  if (process.env.NODE_ENV !== "production") {
    const vite = await createViteServer({
      server: { middlewareMode: true },
      appType: "spa",
    });
    app.use(vite.middlewares);
  } else {
    // Serve production static assets
    const distPath = path.resolve(process.cwd(), "dist");
    app.use(express.static(distPath));
    app.get("*", (req, res) => {
      res.sendFile(path.resolve(distPath, "index.html"));
    });
  }

  app.listen(PORT, "0.0.0.0", () => {
    console.log(`[ALP Astrology CRM Backend] Running on http://0.0.0.0:${PORT}`);
  });
}

start();
