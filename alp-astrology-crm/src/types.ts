/**
 * ALP Astrology CRM - TypeScript Domain Entities
 * Fulfills core database entities requirements
 */

export type UserRole =
  | "Super Admin"
  | "Admin"
  | "Trainer"
  | "Astrologer"
  | "Receptionist"
  | "Social Media Manager"
  | "Account Manager"
  | "Student"
  | "Community Member";

export interface User {
  id: string;
  name: string;
  email: string;
  role: UserRole;
  phone?: string;
  avatar?: string;
}

export type LeadSource =
  | "Website"
  | "WhatsApp"
  | "Instagram"
  | "Facebook"
  | "YouTube"
  | "Google Forms"
  | "Referral"
  | "Manual Entry";

export type LeadStatus =
  | "New Lead"
  | "Contacted"
  | "Interested"
  | "Follow-up Required"
  | "Consultation Booked"
  | "Course Interested"
  | "Converted"
  | "Lost";

export interface Lead {
  id: string;
  name: string;
  mobile: string;
  email: string;
  country: string;
  city: string;
  language: string;
  source: LeadSource;
  interestType: "Consultation" | "Course" | "Workshop" | "Membership" | "Other";
  status: LeadStatus;
  notes: string;
  assignedStaff: string; // User ID or name
  createdAt: string;
  updatedAt: string;
}

export interface LeadActivity {
  id: string;
  leadId: string;
  staffName: string;
  actionType: "Call" | "WhatsApp" | "Email" | "Status Change" | "Note Added";
  details: string;
  timestamp: string;
}

export interface Client {
  id: string;
  name: string;
  phone: string;
  email: string;
  dob: string; // YYYY-MM-DD
  birthTime: string; // HH:MM
  birthPlace: string;
  address: string;
  occupation: string;
  country: string;
  language: string;
}

export type ConsultationType =
  | "Career"
  | "Marriage"
  | "Business"
  | "Health"
  | "Education"
  | "Foreign Settlement"
  | "General Guidance"
  | "Financial Prosperity";

export type ConsultationStatus = "Scheduled" | "Completed" | "Cancelled" | "No Show";

export interface Consultation {
  id: string;
  clientId: string;
  clientName: string; // Denormalized for quick frontend render
  type: ConsultationType;
  dateTime: string; // ISO String
  status: ConsultationStatus;
  astrologerId: string;
  astrologerName: string;
  meetingLink: string; // Zoom or Google Meet
  recordingLink?: string;
  followUpDate?: string;
  followUpNotes?: string;
}

export interface ConsultationNote {
  id: string;
  consultationId: string;
  writerName: string;
  title: string;
  content: string;
  timestamp: string;
}

export interface Course {
  id: string;
  name: string;
  code: string;
  description: string;
  durationWeeks: number;
  fees: number;
}

export interface Batch {
  id: string;
  courseId: string;
  courseName: string;
  name: string; // e.g. Batch A - Jan 2026
  trainerId: string;
  trainerName: string;
  startDate: string;
  schedule: string; // e.g. Saturdays 5PM - 7PM IST
  status: "Upcoming" | "Active" | "Completed";
}

export interface Student {
  id: string;
  name: string;
  photo?: string;
  phone: string;
  email: string;
  country: string;
  occupation: string;
  joinedDate: string;
}

export interface StudentEnrollment {
  id: string;
  studentId: string;
  studentName: string;
  courseId: string;
  courseName: string;
  batchId: string;
  batchName: string;
  enrollmentDate: string;
  progressPercentage: number; // 0 - 100
  attendancePercentage: number; // 0 - 100
  status: "In Progress" | "Completed" | "Dropped";
}

export interface AttendanceRecord {
  id: string;
  enrollmentId: string;
  batchId: string;
  date: string;
  status: "Present" | "Absent" | "Excused";
}

export interface Payment {
  id: string;
  payerName: string;
  payerEmail: string;
  type: "Consultation Fees" | "Course Fees" | "Events" | "Memberships";
  amount: number;
  method: "Online (Stripe)" | "UPI (GPay/PhonePe)" | "Bank Transfer" | "Cash";
  status: "Paid" | "Pending" | "Refunded";
  date: string;
  invoiceId?: string;
}

export interface Invoice {
  id: string;
  paymentId: string;
  invoiceNo: string;
  clientName: string;
  clientEmail: string;
  clientAddress?: string;
  itemName: string;
  amount: number;
  taxAmount: number;
  totalAmount: number;
  issuedDate: string;
  dueDate: string;
  status: "Paid" | "Unpaid" | "Overdue";
}

export interface Certificate {
  id: string;
  studentId: string;
  studentName: string;
  courseName: string;
  certificateNo: string;
  issueDate: string;
  type: "Course Certificate" | "Workshop Certificate" | "Achievement Certificate";
  qrData: string; // verification url
  verifiedAt?: string;
}

export interface Event {
  id: string;
  title: string;
  description: string;
  dateTime: string;
  venueType: "Online" | "Offline";
  venueDetails: string; // Zoom link or physical address
  ticketPrice: number;
  totalSeats: number;
  seatsBooked: number;
}

export interface EventRegistration {
  id: string;
  eventId: string;
  eventTitle: string;
  memberName: string;
  memberEmail: string;
  bookingDate: string;
  ticketCount: number;
  amountPaid: number;
  checkedIn: boolean;
}

export interface CommunityMember {
  id: string;
  name: string;
  email: string;
  category: "Student" | "Consultant" | "Researcher" | "Volunteer" | "Premium Member";
  activeGroup: string;
  location: string;
  joinedDate: string;
}

export interface SocialMediaPost {
  id: string;
  platform: "YouTube" | "Instagram" | "Facebook" | "Threads" | "ShareChat";
  title: string;
  scheduledDate: string; // YYYY-MM-DD
  status: "Draft" | "Scheduled" | "Published";
  campaignName: string;
  clicks: number;
  leadsGenerated: number;
}

export interface AuditLog {
  id: string;
  userId: string;
  userName: string;
  userRole: UserRole;
  action: string;
  target: string;
  timestamp: string;
}

export interface Task {
  id: string;
  title: string;
  assignedTo: string;
  dueDate: string;
  status: "Pending" | "In Progress" | "Completed";
  priority: "Low" | "Medium" | "High";
  leadId?: string;
  isReminder?: boolean;
  reminderTime?: string;
}

export interface Notification {
  id: string;
  title: string;
  message: string;
  timestamp: string;
  read: boolean;
}

export interface Employee {
  id: string;
  name: string;
  email: string;
  password?: string;
  role: UserRole;
  phone?: string;
  avatar?: string;
  createdTime: string;
  status: "Active" | "Suspended" | "On Leave";
}

