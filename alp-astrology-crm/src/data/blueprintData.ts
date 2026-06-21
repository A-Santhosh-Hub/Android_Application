/**
 * ALP Astrology CRM - Software Architecture and System Blueprints
 * Fulfills output requirements: Architecture, Database Design, API docs, Roadmaps, etc.
 */

export interface SchemaField {
  name: string;
  type: string;
  constraints: string;
  description: string;
}

export interface SchemaTable {
  name: string;
  columns: SchemaField[];
  indexes: string[];
  optimization: string;
  foreignKeys: string[];
}

export interface BlueprintData {
  architecture: {
    title: string;
    description: string;
    stack: { layer: string; technology: string; rationale: string }[];
    layers: { name: string; details: string[] }[];
    securityModel: string[];
  };
  tables: SchemaTable[];
  apiRoutes: {
    module: string;
    routes: { method: "GET" | "POST" | "PUT" | "DELETE"; path: string; request: string; response: string; desc: string }[];
  }[];
  folderStructure: string;
  wireframes: {
    screenName: string;
    layout: string;
    elements: string[];
  }[];
  userFlows: {
    name: string;
    steps: string[];
  }[];
  roadmap: {
    phase: string;
    duration: string;
    milestones: string[];
    deliverables: string[];
  }[];
  sprints: {
    sprint: string;
    focus: string;
    backlog: string[];
  }[];
  deploymentPlan: {
    step: string;
    command: string;
    notes: string;
  }[];
}

export const blueprintData: BlueprintData = {
  architecture: {
    title: "ALP Astrology CRM - Enterprise Multi-Tenant Architecture",
    description:
      "A high-availability, microservices-ready layered enterprise architecture designed for 100,000+ global users (astrologers, trainers, and customers). Designed to securely handle high-traffic student enrollment, online consultation video routing, and real-time ALP astronomical math utilities.",
    stack: [
      {
        layer: "Frontend App Layer",
        technology: "React 19 + TypeScript + Vite 6 + Tailwind CSS",
        rationale: "Blazing fast SPA compilation, headless Tailwind layouts, fluid role-based navigations, and direct offline state syncing."
      },
      {
        layer: "Reverse Proxy & TLS",
        technology: "Nginx reverse proxy + certbot Let's Encrypt",
        rationale: "Terminates HTTPS, proxies port 3000, routes static files, restricts CORS headers, and handles HTTP rate limiting."
      },
      {
        layer: "Enterprise API Layer",
        technology: "Node.js Custom Express + tsx + esbuild transpiler",
        rationale: "Seamless integration with astro computations, light memory footprint, fast cold starts on Cloud Run containers."
      },
      {
        layer: "Relational DB Layer",
        technology: "MySQL / Cloud SQL (PostgreSQL compatible schema-agnostic layers)",
        rationale: "Normalized ACID tabular engine containing complete transactional logs of consultation payments, student attendance records, and certificates."
      },
      {
        layer: "Caching & Sessions",
        technology: "Redis Memory Store",
        rationale: "Speeds up token validations, rates-limits API routes, and caches astronomical Lagna (LCP) coordinate maps."
      },
      {
        layer: "ASTRO Computations",
        technology: "Swiss Ephemeris / Astrology SDK Integrations",
        rationale: "Accurate planetary degree positions for DOB and birthplace calculations, used in dynamic Akshaya Lagna calculations."
      }
    ],
    layers: [
      {
        name: "Presentation Layer (Client)",
        details: [
          "Responsive React view modules segmented by User Roles (Admin, Astrologer, Trainer, Student).",
          "Local state synchronized with REST backend via token-authenticated axios hooks.",
          "Tailwind theme config with custom typography ('Inter' + 'JetBrains Mono') and astrological accent colors."
        ]
      },
      {
        name: "Security & Middleware Gateway",
        details: [
          "JWT Token Authentication with cookie storage and Bearer headers.",
          "Role-Based Access Control (RBAC) validations enforced on all REST endpoints.",
          "CORS restrictions: exclusively accepts main application domains and trusted third-party webhooks."
        ]
      },
      {
        name: "Business Logic Broker (Server)",
        details: [
          "Lead Assignment Engine: automatically assigns web/WhatsApp leads based on astrologer specialty and current workload.",
          "ALP Lagna Calculator Engine: calculates precise rising sign progression based on birth time (years elapsed since birth).",
          "Certificate PDF Generator & QR Signer: signs credentials using a private RSA key, outputting quick verification hashes."
        ]
      },
      {
        name: "Infrastructure & File System",
        details: [
          "AWS S3 bucket / Cloudinary: persists high-resolution student profile photos, session video archives, and coursework submissions.",
          "Database Connection Pooling: manages 150+ steady persistent connections with regional replicas for immediate scaling."
        ]
      }
    ],
    securityModel: [
      "All client transmission encrypted with TLS v1.3.",
      "Sensitive database columns (e.g. passwords, secure notes) salted and hashed with SHA-256 / bcrypt.",
      "Comprehensive Audit logger: every write action records User ID, IP, and previous state for ISO compliance.",
      "API Rate limiter: limits IP addresses to a max of 100 requests per minute on general routes, and 5 requests per minute on authentication handlers."
    ]
  },
  tables: [
    {
      name: "users",
      columns: [
        { name: "id", type: "VARCHAR(36)", constraints: "PRIMARY KEY", description: "UUID identifier of account" },
        { name: "name", type: "VARCHAR(100)", constraints: "NOT NULL", description: "Legal name of user" },
        { name: "email", type: "VARCHAR(150)", constraints: "UNIQUE, INDEX", description: "Primary login identification" },
        { name: "password_hash", type: "VARCHAR(255)", constraints: "NOT NULL", description: "Bcrypt securely hashed password" },
        { name: "role_id", type: "VARCHAR(36)", constraints: "FOREIGN KEY", description: "Reference value to roles" },
        { name: "phone", type: "VARCHAR(20)", constraints: "NULL", description: "Contact number with country code" },
        { name: "status", type: "ENUM('Active','Inactive','Suspended')", constraints: "DEFAULT 'Active'", description: "Access state" },
        { name: "created_at", type: "TIMESTAMP", constraints: "DEFAULT NOW()", description: "Audit creation date" }
      ],
      indexes: ["idx_users_email", "idx_users_status"],
      optimization: "Composite index on (email, status) to drastically speed up email authentication lookup queries.",
      foreignKeys: ["role_id -> roles.id"]
    },
    {
      name: "roles",
      columns: [
        { name: "id", type: "VARCHAR(36)", constraints: "PRIMARY KEY", description: "Unique UUID identifier" },
        { name: "name", type: "VARCHAR(50)", constraints: "UNIQUE, NOT NULL", description: "Role label (e.g. Super Admin, Astrologer)" },
        { name: "description", type: "TEXT", constraints: "NULL", description: "Boundary description of access" }
      ],
      indexes: [],
      optimization: "Table is small (<20 records), kept in-memory cache directly on Express gateways to eliminate redundant database hits.",
      foreignKeys: []
    },
    {
      name: "permissions",
      columns: [
        { name: "id", type: "VARCHAR(36)", constraints: "PRIMARY KEY", description: "UUID identifier" },
        { name: "role_id", type: "VARCHAR(36)", constraints: "FOREIGN KEY, INDEX", description: "Reference roles" },
        { name: "module", type: "VARCHAR(50)", constraints: "NOT NULL", description: "Target core module (e.g. leads, courses)" },
        { name: "action", type: "ENUM('create','read','update','delete','all')", constraints: "NOT NULL", description: "Action limit boundary" }
      ],
      indexes: ["idx_permissions_role_id_module"],
      optimization: "Composite index to quickly evaluate RBAC authorization during controller invocation step.",
      foreignKeys: ["role_id -> roles.id"]
    },
    {
      name: "leads",
      columns: [
        { name: "id", type: "VARCHAR(36)", constraints: "PRIMARY KEY", description: "Lead identifier" },
        { name: "name", type: "VARCHAR(100)", constraints: "NOT NULL", description: "Name of prospect" },
        { name: "mobile", type: "VARCHAR(20)", constraints: "INDEX, NOT NULL", description: "Mobile contact for SMS/WhatsApp verification" },
        { name: "email", type: "VARCHAR(150)", constraints: "NULL", description: "Email of prospect" },
        { name: "country", type: "VARCHAR(100)", constraints: "NOT NULL", description: "Country of residence" },
        { name: "city", type: "VARCHAR(100)", constraints: "NULL", description: "City origin" },
        { name: "language", type: "VARCHAR(50)", constraints: "DEFAULT 'Tamil'", description: "Astrological communication language preferred" },
        { name: "source", type: "VARCHAR(50)", constraints: "INDEX", description: "Origin (e.g. WhatsApp, FB Ads, Website)" },
        { name: "interest_type", type: "ENUM('Consultation','Course','Workshop','Membership','Other')", constraints: "NOT NULL", description: "Target product funnel" },
        { name: "status", type: "VARCHAR(50)", constraints: "INDEX", description: "Funnel phase status" },
        { name: "notes", type: "TEXT", constraints: "NULL", description: "Initial requirement logs" },
        { name: "assigned_staff_id", type: "VARCHAR(36)", constraints: "FOREIGN KEY, NULL", description: "Reference to staff member servicing" },
        { name: "created_at", type: "TIMESTAMP", constraints: "DEFAULT NOW()", description: "Time captured" }
      ],
      indexes: ["idx_leads_mobile", "idx_leads_status", "idx_leads_created_at"],
      optimization: "Clustered index scan alignment on created_at to serve fast dashboard charts.",
      foreignKeys: ["assigned_staff_id -> users.id"]
    },
    {
      name: "clients",
      columns: [
        { name: "id", type: "VARCHAR(36)", constraints: "PRIMARY KEY", description: "Client UUID" },
        { name: "name", type: "VARCHAR(100)", constraints: "NOT NULL", description: "Client Legal Name" },
        { name: "phone", type: "VARCHAR(20)", constraints: "INDEX, NOT NULL", description: "Active mobile phone" },
        { name: "email", type: "VARCHAR(150)", constraints: "NULL", description: "Active primary email" },
        { name: "dob", type: "DATE", constraints: "NOT NULL", description: "Birthdate for planetary charts (YYYY-MM-DD)" },
        { name: "birth_time", type: "TIME", constraints: "NOT NULL", description: "Exact hours/minutes" },
        { name: "birth_place", type: "VARCHAR(150)", constraints: "NOT NULL", description: "City/coordinate origin for local houses" },
        { name: "address", type: "TEXT", constraints: "NULL", description: "Postal location" },
        { name: "occupation", type: "VARCHAR(100)", constraints: "NULL", description: "Profession focus point" },
        { name: "country", type: "VARCHAR(100)", constraints: "NOT NULL", description: "Country of residence" }
      ],
      indexes: ["idx_clients_phone", "idx_clients_name"],
      optimization: "Prefix indexes set on client names for instant autocompletion inside consultation forms.",
      foreignKeys: []
    },
    {
      name: "consultations",
      columns: [
        { name: "id", type: "VARCHAR(36)", constraints: "PRIMARY KEY", description: "Consultation UUID" },
        { name: "client_id", type: "VARCHAR(36)", constraints: "FOREIGN KEY, NOT NULL, INDEX", description: "Clients link" },
        { name: "type", type: "VARCHAR(50)", constraints: "NOT NULL", description: "Career, marriage, health, foreign travel focus" },
        { name: "date_time", type: "DATETIME", constraints: "NOT NULL, INDEX", description: "Scheduled meeting date" },
        { name: "status", type: "VARCHAR(50)", constraints: "NOT NULL", description: "Scheduled, Completed, Cancelled" },
        { name: "astrologer_id", type: "VARCHAR(36)", constraints: "FOREIGN KEY, INDEX", description: "User ID performing analysis" },
        { name: "meeting_link", type: "VARCHAR(255)", constraints: "NOT NULL", description: "Teleconference meeting URL" },
        { name: "recording_link", type: "VARCHAR(255)", constraints: "NULL", description: "Cloud recording location" },
        { name: "follow_up_date", type: "DATE", constraints: "NULL", description: "Future appointment schedule" },
        { name: "follow_up_notes", type: "TEXT", constraints: "NULL", description: "Instructions" }
      ],
      indexes: ["idx_cons_date_time", "idx_cons_astrologer"],
      optimization: "Composite index on (astrologer_id, date_time) to speed up calendar scheduler conflicts checking.",
      foreignKeys: ["client_id -> clients.id", "astrologer_id -> users.id"]
    },
    {
      name: "consultation_notes",
      columns: [
        { name: "id", type: "VARCHAR(36)", constraints: "PRIMARY KEY", description: "Self ID" },
        { name: "consultation_id", type: "VARCHAR(36)", constraints: "FOREIGN KEY, NOT NULL", description: "Target meeting" },
        { name: "writer_name", type: "VARCHAR(100)", constraints: "NOT NULL", description: "Staff name" },
        { name: "title", type: "VARCHAR(150)", constraints: "NOT NULL", description: "Section name" },
        { name: "content", type: "TEXT", constraints: "NOT NULL", description: "Sensitive Astro reading notes" },
        { name: "timestamp", type: "TIMESTAMP", constraints: "DEFAULT NOW()", description: "Time written" }
      ],
      indexes: [],
      optimization: "Encrypted at-rest. Uses relational mapping to load only on specific client file open.",
      foreignKeys: ["consultation_id -> consultations.id"]
    },
    {
      name: "courses",
      columns: [
        { name: "id", type: "VARCHAR(36)", constraints: "PRIMARY KEY", description: "Course Unique ID" },
        { name: "name", type: "VARCHAR(150)", constraints: "NOT NULL", description: "Course title (ALP Basic, ALP Pro)" },
        { name: "code", type: "VARCHAR(30)", constraints: "UNIQUE, INDEX", description: "Academic code" },
        { name: "description", type: "TEXT", constraints: "NULL", description: "Syllabus contents" },
        { name: "duration_weeks", type: "INT", constraints: "NOT NULL", description: "Module timeline" },
        { name: "fees", type: "DECIMAL(10,2)", constraints: "NOT NULL", description: "Total price amount" }
      ],
      indexes: ["idx_courses_code"],
      optimization: "Courses table is heavily read, structured with static caching on DNS network-edges to save DB capacity.",
      foreignKeys: []
    },
    {
      name: "batches",
      columns: [
        { name: "id", type: "VARCHAR(36)", constraints: "PRIMARY KEY", description: "Batch UUID" },
        { name: "course_id", type: "VARCHAR(36)", constraints: "FOREIGN KEY, INDEX", description: "Parent course" },
        { name: "name", type: "VARCHAR(100)", constraints: "NOT NULL", description: "Batch designator (e.g. Batch B - Summer)" },
        { name: "trainer_id", type: "VARCHAR(36)", constraints: "FOREIGN KEY, INDEX", description: "Trainer Assigned user" },
        { name: "start_date", type: "DATE", constraints: "NOT NULL", description: "Initial session date" },
        { name: "schedule", type: "VARCHAR(150)", constraints: "NOT NULL", description: "Physical timing" },
        { name: "status", type: "VARCHAR(50)", constraints: "DEFAULT 'Upcoming'", description: "Active or finished" }
      ],
      indexes: ["idx_batches_course_id", "idx_batches_trainer"],
      optimization: "Indexes speed up dashboard load for Trainers on login.",
      foreignKeys: ["course_id -> courses.id", "trainer_id -> users.id"]
    },
    {
      name: "students",
      columns: [
        { name: "id", type: "VARCHAR(36)", constraints: "PRIMARY KEY", description: "Student identifier keys" },
        { name: "name", type: "VARCHAR(100)", constraints: "NOT NULL", description: "Legal name" },
        { name: "photo_url", type: "VARCHAR(255)", constraints: "NULL", description: "Link to user profile image on AWS" },
        { name: "phone", type: "VARCHAR(20)", constraints: "INDEX, NOT NULL", description: "Call phone" },
        { name: "email", type: "VARCHAR(150)", constraints: "UNIQUE, INDEX", description: "Academics contact" },
        { name: "country", type: "VARCHAR(100)", constraints: "NOT NULL", description: "National origin" },
        { name: "occupation", type: "VARCHAR(100)", constraints: "NULL", description: "Socio-professional orientation" },
        { name: "joined_date", type: "DATE", constraints: "NOT NULL", description: "Initial onboarding time" }
      ],
      indexes: ["idx_students_phone", "idx_students_email"],
      optimization: "Phone and email indexed to prevent double register profiles on courses checkout.",
      foreignKeys: []
    },
    {
      name: "student_enrollments",
      columns: [
        { name: "id", type: "VARCHAR(36)", constraints: "PRIMARY KEY", description: "Link identifier" },
        { name: "student_id", type: "VARCHAR(36)", constraints: "FOREIGN KEY, INDEX", description: "Student mapping value" },
        { name: "course_id", type: "VARCHAR(36)", constraints: "FOREIGN KEY", description: "Course mapping" },
        { name: "batch_id", type: "VARCHAR(36)", constraints: "FOREIGN KEY, INDEX", description: "Specific Batch mapping" },
        { name: "enrollment_date", type: "DATE", constraints: "NOT NULL", description: "Date checkout completed" },
        { name: "progress_percentage", type: "INT", constraints: "DEFAULT 0", description: "Academic tracking" },
        { name: "attendance_percentage", type: "INT", constraints: "DEFAULT 0", description: "Attendance calculation stats" },
        { name: "status", type: "VARCHAR(50)", constraints: "DEFAULT 'In Progress'", description: "Completed, Dropped status" }
      ],
      indexes: ["idx_enroll_student_id", "idx_enroll_batch_id"],
      optimization: "Ensures quick lookup of current classmates inside Trainer dashboards.",
      foreignKeys: ["student_id -> students.id", "course_id -> courses.id", "batch_id -> batches.id"]
    },
    {
      name: "attendance",
      columns: [
        { name: "id", type: "VARCHAR(36)", constraints: "PRIMARY KEY", description: "Record ID" },
        { name: "enrollment_id", type: "VARCHAR(36)", constraints: "FOREIGN KEY, INDEX", description: "Match to student path" },
        { name: "batch_id", type: "VARCHAR(36)", constraints: "FOREIGN KEY", description: "Batch class focus" },
        { name: "date", type: "DATE", constraints: "NOT NULL", description: "Attendance sheet date" },
        { name: "status", type: "ENUM('Present','Absent','Excused')", constraints: "NOT NULL", description: "Status check" }
      ],
      indexes: ["idx_att_enrollment_id_date"],
      optimization: "Composite index helps query precise trends dynamically and recalculates student percentages quickly.",
      foreignKeys: ["enrollment_id -> student_enrollments.id", "batch_id -> batches.id"]
    },
    {
      name: "payments",
      columns: [
        { name: "id", type: "VARCHAR(36)", constraints: "PRIMARY KEY", description: "Payment transaction UUID" },
        { name: "payer_name", type: "VARCHAR(150)", constraints: "NOT NULL", description: "Invoice billing name" },
        { name: "payer_email", type: "VARCHAR(150)", constraints: "NOT NULL", description: "Payer contact" },
        { name: "type", type: "VARCHAR(50)", constraints: "INDEX", description: "Consultation, Courses, Events, Memberships" },
        { name: "amount", type: "DECIMAL(10,2)", constraints: "NOT NULL", description: "Collected value" },
        { name: "method", type: "VARCHAR(50)", constraints: "NOT NULL", description: "UPI, Stripe gateway, bank, cash" },
        { name: "status", type: "VARCHAR(50)", constraints: "INDEX", description: "Paid, Pending, Refunded" },
        { name: "date", type: "DATE", constraints: "INDEX", description: "Payment processing date" },
        { name: "invoice_id", type: "VARCHAR(36)", constraints: "NULL", description: "Linked matching invoice" }
      ],
      indexes: ["idx_payments_date", "idx_payments_status"],
      optimization: "Range partitions mapping on payments.date field secures quick queries across annual financial audits.",
      foreignKeys: []
    },
    {
      name: "invoices",
      columns: [
        { name: "id", type: "VARCHAR(36)", constraints: "PRIMARY KEY", description: "Invoice UUID" },
        { name: "payment_id", type: "VARCHAR(36)", constraints: "FOREIGN KEY", description: "Source payment link" },
        { name: "invoice_no", type: "VARCHAR(50)", constraints: "UNIQUE, INDEX", description: "Formatted bill series (e.g. ALP-2026-0034)" },
        { name: "client_name", type: "VARCHAR(100)", constraints: "NOT NULL", description: "Tax address name" },
        { name: "client_email", type: "VARCHAR(150)", constraints: "NOT NULL", description: "Tax address email" },
        { name: "item_name", type: "VARCHAR(255)", constraints: "NOT NULL", description: "Item bought desc" },
        { name: "amount", type: "DECIMAL(10,2)", constraints: "NOT NULL", description: "Pre-tax total value" },
        { name: "tax_amount", type: "DECIMAL(10,2)", constraints: "NOT NULL", description: "GST computed value" },
        { name: "total_amount", type: "DECIMAL(10,2)", constraints: "NOT NULL", description: "Grand total" },
        { name: "issued_date", type: "DATE", constraints: "NOT NULL", description: "Print date" },
        { name: "due_date", type: "DATE", constraints: "NOT NULL", description: "Expiration deadline" },
        { name: "status", type: "VARCHAR(50)", constraints: "DEFAULT 'Paid'", description: "Paid, Unpaid, Overdue" }
      ],
      indexes: ["idx_inv_no"],
      optimization: "Indexes unique invoice series to allow immediate global string searching in Accounts.",
      foreignKeys: ["payment_id -> payments.id"]
    },
    {
      name: "certificates",
      columns: [
        { name: "id", type: "VARCHAR(36)", constraints: "PRIMARY KEY", description: "Certificate UUID" },
        { name: "student_id", type: "VARCHAR(36)", constraints: "FOREIGN KEY, INDEX", description: "Receiver reference" },
        { name: "student_name", type: "VARCHAR(100)", constraints: "NOT NULL", description: "Name printed exactly" },
        { name: "course_name", type: "VARCHAR(150)", constraints: "NOT NULL", description: "Course completed" },
        { name: "certificate_no", type: "VARCHAR(50)", constraints: "UNIQUE, INDEX", description: "Hash signature for verification" },
        { name: "issue_date", type: "DATE", constraints: "NOT NULL", description: "Printed date" },
        { name: "type", type: "VARCHAR(50)", constraints: "NOT NULL", description: "Achiever level status" },
        { name: "qr_data", type: "TEXT", constraints: "NOT NULL", description: "Encrypted link matching verification" }
      ],
      indexes: ["idx_cert_no"],
      optimization: "QR data maps to a static route ensuring external requests verifying credentials bypass full ORM stack.",
      foreignKeys: ["student_id -> students.id"]
    },
    {
      name: "events",
      columns: [
        { name: "id", type: "VARCHAR(36)", constraints: "PRIMARY KEY", description: "Event UUID" },
        { name: "title", type: "VARCHAR(150)", constraints: "NOT NULL", description: "Event Title" },
        { name: "description", type: "TEXT", constraints: "NULL", description: "Brief promotional notes" },
        { name: "date_time", type: "DATETIME", constraints: "INDEX, NOT NULL", description: "Schedule time" },
        { name: "venue_type", type: "ENUM('Online','Offline')", constraints: "NOT NULL", description: "Location setting class" },
        { name: "venue_details", type: "VARCHAR(255)", constraints: "NOT NULL", description: "Link or address" },
        { name: "ticket_price", type: "DECIMAL(10,2)", constraints: "DEFAULT 0.00", description: "Entrance fee" },
        { name: "total_seats", type: "INT", constraints: "NOT NULL", description: "Limit capacity" },
        { name: "seats_booked", type: "INT", constraints: "DEFAULT 0", description: "Occupied tracker value" }
      ],
      indexes: ["idx_events_date_time"],
      optimization: "Used for real-time frontend calendars.",
      foreignKeys: []
    },
    {
      name: "event_registrations",
      columns: [
        { name: "id", type: "VARCHAR(36)", constraints: "PRIMARY KEY", description: "Reg UUID" },
        { name: "event_id", type: "VARCHAR(36)", constraints: "FOREIGN KEY, INDEX", description: "Target event" },
        { name: "member_name", type: "VARCHAR(100)", constraints: "NOT NULL", description: "Holder name" },
        { name: "member_email", type: "VARCHAR(150)", constraints: "NOT NULL", description: "Holder email" },
        { name: "booking_date", type: "DATE", constraints: "NOT NULL", description: "Reservation checkout time" },
        { name: "ticket_count", type: "INT", constraints: "DEFAULT 1", description: "Guests registered count" },
        { name: "amount_paid", type: "DECIMAL(10,2)", constraints: "NOT NULL", description: "Cash collected value" },
        { name: "checked_in", type: "BOOLEAN", constraints: "DEFAULT FALSE", description: "Physical gate control tick" }
      ],
      indexes: ["idx_reg_event_id"],
      optimization: "Optimized for gate check-in scanning using ticket references.",
      foreignKeys: ["event_id -> events.id"]
    },
    {
      name: "community_members",
      columns: [
        { name: "id", type: "VARCHAR(36)", constraints: "PRIMARY KEY", description: "Member UUID" },
        { name: "name", type: "VARCHAR(100)", constraints: "NOT NULL", description: "Full name" },
        { name: "email", type: "VARCHAR(150)", constraints: "UNIQUE, INDEX", description: "Direct communication address" },
        { name: "category", type: "VARCHAR(50)", constraints: "INDEX", description: "Students, Consultants, researchers" },
        { name: "active_group", type: "VARCHAR(100)", constraints: "NOT NULL", description: "Group location channel title" },
        { name: "location", type: "VARCHAR(100)", constraints: "NOT NULL", description: "Geo regional hub coordinates" },
        { name: "joined_date", type: "DATE", constraints: "NOT NULL", description: "Onboarding Date" }
      ],
      indexes: ["idx_cm_category"],
      optimization: "Keeps rapid filters of directory search engines blazing fast.",
      foreignKeys: []
    },
    {
      name: "social_media_posts",
      columns: [
        { name: "id", type: "VARCHAR(36)", constraints: "PRIMARY KEY", description: "Post UUID" },
        { name: "platform", type: "VARCHAR(50)", constraints: "INDEX", description: "YouTube, Instagram, Facebook" },
        { name: "title", type: "VARCHAR(255)", constraints: "NOT NULL", description: "Campaign post topic heading" },
        { name: "scheduled_date", type: "DATE", constraints: "INDEX", description: "Date of release trigger" },
        { name: "status", type: "VARCHAR(50)", constraints: "DEFAULT 'Draft'", description: "Draft, Scheduled, Published" },
        { name: "campaign_name", type: "VARCHAR(100)", constraints: "NULL", description: "Target marketing campaign" },
        { name: "clicks", type: "INT", constraints: "DEFAULT 0", description: "Traffic tracking analytical metric" },
        { name: "leads_generated", type: "INT", constraints: "DEFAULT 0", description: "CRM performance tracking value" }
      ],
      indexes: ["idx_smp_date_status"],
      optimization: "Composite index on (scheduled_date, status) to run scheduled cron jobs executing publisher API.",
      foreignKeys: []
    }
  ],
  apiRoutes: [
    {
      module: "Authentication & Security",
      routes: [
        { method: "POST", path: "/api/auth/login", request: "{ email, password }", response: "{ token, user: { id, name, role } }", desc: "Validates system credentials, signs and issues stateless JWT encryption." },
        { method: "GET", path: "/api/auth/me", request: "Headers: { Bearer Token }", response: "{ authenticated: true, user: { id, role } }", desc: "Decodes authorization context across SPA views and restricts panels." }
      ]
    },
    {
      module: "Leads Management CRM",
      routes: [
        { method: "GET", path: "/api/leads", request: "Query: { status, source, page, limit }", response: "{ leads: Lead[], total: number }", desc: "Retrieves complete lead pipelines, supporting advanced sorting." },
        { method: "POST", path: "/api/leads", request: "{ name, mobile, email, source, interestType }", response: "Lead", desc: "Endpoint used by external Google Forms and website webhooks." },
        { method: "PUT", path: "/api/leads/:id", request: "{ status, notes, assignedStaff }", response: "Lead", desc: "Performs incremental record edits and logs pipeline progress change." },
        { method: "POST", path: "/api/leads/:id/activity", request: "{ actionType, details }", response: "LeadActivity", desc: "Appends call histories or WhatsApp template trigger recordings to timeline." }
      ]
    },
    {
      module: "Consultation Scheduling & Lagna API",
      routes: [
        { method: "GET", path: "/api/consultations", request: "Query: { astrologerId, fromDate, toDate }", response: "Consultation[]", desc: "Retrieves scheduled appointments for calendar populating." },
        { method: "POST", path: "/api/consultations", request: "{ clientId, type, dateTime, astrologerId }", response: "Consultation", desc: "Schedules calendar bookings and registers auto Zoom sessions." },
        { method: "POST", path: "/api/astrology/lagna-calculate", request: "{ dob, birthTime, birthPlace, calculationPoints }", response: "{ akshayaLagnaSign, housePositions: [], planetaryDegrees: {} }", desc: "Enterprise mathematical planetary degrees calculator for ALP astrological chart rendering." }
      ]
    },
    {
      module: "Student Program & Courses Tracker",
      routes: [
        { method: "GET", path: "/api/courses", request: "None", response: "Course[]", desc: "Fetches full curriculum schedules." },
        { method: "GET", path: "/api/students", request: "Query: { searchQuery, state }", response: "Student[]", desc: "Comprehensive Student search index." },
        { method: "POST", path: "/api/students/enroll", request: "{ studentId, courseId, batchId }", response: "StudentEnrollment", desc: "Registers student inside the structural batch pipeline, preparing fee balances." },
        { method: "POST", path: "/api/attendance/batch", request: "{ batchId, date, records: [{ enrollmentId, status }] }", response: "{ success: true }", desc: "Uploads daily class roll sheet." }
      ]
    }
  ],
  folderStructure: `/.env.example
/.gitignore
/index.html
/metadata.json
/package.json
/tsconfig.json
/vite.config.ts
/server.ts                    // Custom full-stack Express + Vite server config
/dist/server.cjs              // Production esbuild bundled server executable
/src/
  /main.tsx                   // React DOM initialization and root mount
  /index.css                  // Global Tailwind imports & typography setup
  /types.ts                   // Centralized custom SQL-to-TypeScript domain interfaces
  /App.tsx                    // Route orchestrator, core state holder & dark theme
  /data/
    /blueprintData.ts         // Enterprise documentation details (Architecture, Sprints)
  /components/
    /Sidebar.tsx              // Professional role-based sidebar and selector
    /DashboardOverview.tsx    // Enterprise stats bento-grid & follow-up task panels
    /LeadManagement.tsx       // Interactive lead boards, capture forms & call history
    /ConsultationManagement.tsx // Astro lagna wheel plotter, Google Meet scheduling card
    /CourseStudentManagement.tsx// Class portfolios, student profiles & attendance tracking
    /PaymentManagement.tsx    // Revenue books, invoice generation engine & receipts
    /CertificateManagement.tsx// QR validation platform & dynamic credential printer
    /CommunityEventSocial.tsx // Community roster, scheduled events & social post plans
    /ReportsAnalytics.tsx     // Advanced filters & exportable system tracking reports
    /ArchitectureSuite.tsx    // Live graphical ER-diagram, API console, Sprint schedule`,
  wireframes: [
    {
      screenName: "Enterprise Overview Dashboard",
      layout: "Header (Role switch, Global Search, Notify badge) | Sidebar (Dynamic nav shortcuts) | Body Grid (4 major stats counters, Chart view toggler, Today's call tasks, Audit logs)",
      elements: ["Revenue Tracker Chart", "Upcoming Consultation card", "Follow-up priority reminders", "System state summary"]
    },
    {
      screenName: "Lead Kanban Workspace",
      layout: "Top Controls (Filter language, source, country + Generate Lead button) | Pipeline Canvas (A horizontal flex wrap with 8 lists mapped by status) | Kanban Ticket (Lead Card with custom tags for language, source, interest, and clickable activity logs)",
      elements: ["Lead details trigger modal", "Call notes timeline stream", "WhatsApp trigger test API", "Owner assign handler"]
    },
    {
      screenName: "ALP Birth Chart Profiler",
      layout: "Split Screen Grid | Left Side (Birth Coordinates Input Form: DOB, exact time, geographic location coordinates) | Right Side (Akshaya Lagna interactive SVG plot showing current progressed Lagna and house coordinates in circle)",
      elements: ["D1 Birth Chart representation", "Progressed Lagna calculated year marker", "Consultation summary writing block"]
    }
  ],
  userFlows: [
    {
      name: "Prospect to Astrologer Consultation Flow",
      steps: [
        "Lead gets registered automatically (External Google Forms Integration, WhatsApp ping, or API webhook).",
        "Receptionist reviews languages, categorizes interest, and assigns the appropriate expert (Career, Marriage, Health specialized Astrologer).",
        "Lead status moves to 'Contacted'. Call notes and WhatsApp templates get logged in the Lead Activity profile.",
        "Prospect pays Consultation Fees. Payment is logged in Payments table and Invoice is dispatched to Client. Status updates to 'Consultation Booked'.",
        "Client details match automatically created record in client table. Astrologer opens appointment inside Consultation Workspace.",
        "Appointment proceeds over Google Meet. Astrologer inputs precise coordinates, plots Akshaya Lagna progression, takes session notes.",
        "Meeting ends. Astrologer uploads video recording URL, schedules follow-up reminder, and logs prescription remarks in clients profile."
      ]
    },
    {
      name: "Student Enrollment and QR Certificate Issuance Flow",
      steps: [
        "Lead indicates academy interest (Status: 'Course Interested'). Course advisor details ALP syllabus tiers.",
        "Student registers, pays fees. Record is securely created inside students table and maps to CourseEnrollment.",
        "Student assigned to specific Batch (handled by Trainer role).",
        "Trainer records class-by-class roll call attendance (saved directly into the SQL attendance database tables).",
        "Student completes coursework, sits for examination. Grade performance metrics recorded in system database.",
        "On graduation, Trainer triggers 'Issue Certificate'. System automatically generates Unique Certificate ID, records it in the certificates entity table, and signs QR validation URL.",
        "Student downloads credential PDF. Any external user scanning the QR gets redirected to the Certificate Validation Portal verifying authenticity."
      ]
    }
  ],
  roadmap: [
    {
      phase: "Phase 1: Database & Backend Engine Core Setup",
      duration: "Weeks 1-3",
      milestones: ["Define all 24 database schemas", "Provision database clusters with automatic regional replication pools", "Setup JWT auth & middleware routers"],
      deliverables: ["Secured Express REST APIs", "Database clustering setup", "RBAC testing suite"]
    },
    {
      phase: "Phase 2: Lead Pipeline & Astro Calculators",
      duration: "Weeks 4-6",
      milestones: ["Develop leads boards kanban", "Integrate Swiss Ephemeris astronomical math libraries", "Implement Zoom/Google Meet scheduling integrations"],
      deliverables: ["CRM core modules testing build", "Precise Akshaya Lagna charting component", "Interactive scheduling calendar"]
    },
    {
      phase: "Phase 3: Academy, Billing & Community Portal",
      duration: "Weeks 7-9",
      milestones: ["Build course and batch manager boards", "Integrate payment gateways (UPI, Stripe webhook notifications)", "Develop cryptographic certificate QR signers"],
      deliverables: ["Student management portal", "Billing ledger & invoice prints", "Authenticity validator tool"]
    },
    {
      phase: "Phase 4: Optimization, Security Hardening & Release",
      duration: "Weeks 10-12",
      milestones: ["Build reports dashboard with high-performance engines", "Execute penetration tests & restrict CORS", "Configure VPS, docker-compose orchestration, and Nginx proxy configs"],
      deliverables: ["Exportable PDF/Excel reports suite", "Production Docker containers", "ALP Astrology CRM Live Environment"]
    }
  ],
  sprints: [
    {
      sprint: "Sprint 1: Core Portal Setup & IAM (Week 1-2)",
      focus: "Build database instances, define roles, and establish secure auth tokens.",
      backlog: [
        "Create 'users', 'roles', and 'permissions' database schemas",
        "Formulate express server boilerplates",
        "Design standard RBAC authorization token validators",
        "Design polished layout design structure with theme context grids"
      ]
    },
    {
      sprint: "Sprint 2: Lead Pipelines & Interactions (Week 3-4)",
      focus: "Implement prospect capture boards, follow-up timers, and communication trails.",
      backlog: [
        "Create lead forms and kanban panels",
        "Integrate WhatsApp template trigger mock webhooks",
        "Build prospect activities history logs",
        "Design receptionist and admin specific metrics overview grids"
      ]
    },
    {
      sprint: "Sprint 3: Consultation Bookings & Astro Analytics (Week 5-6)",
      focus: "Develop appointment sheets, calendars, and ALP astronomical chart engine.",
      backlog: [
        "Create schedules calendar matching consultations table",
        "Inject core ALP planetary degree math calculation endpoints",
        "Squeeze coordinate lookup UI controls (DOB, latitude, longitude calculations)",
        "Design beautiful circular ASTRO progressed chart widgets"
      ]
    },
    {
      sprint: "Sprint 4: Academics, Payments, and Verification (Week 7-8)",
      focus: "Deploy Course & student registers, dynamic invoicing engines, and validation keys.",
      backlog: [
        "Formulate Courses, Batches, and Students dashboards",
        "Design Payment ledgers, printable HTML invoices and credit checks",
        "Formulate QR verifiable credential certificates and search portal verification queries",
        "Establish community directory pages with filter parameters"
      ]
    },
    {
      sprint: "Sprint 5: System Optimization & Analytics (Week 9-10)",
      focus: "Build visual analytical reports and audit logs tools.",
      backlog: [
        "Create interactive timeline reporting charts",
        "Establish mock CSV/excel reports export utilities",
        "Securely integrate sensitive logs records visual tools to monitor admin activities",
        "Conduct comprehensive cross-browser and mobile responsive styling inspections"
      ]
    }
  ],
  deploymentPlan: [
    {
      step: "1. Install system prerequisites on clean Linux VPS",
      command: "sudo apt update && sudo apt install -y docker.io docker-compose certbot nginx ufw",
      notes: "Enables dockerized containers deployment, SSL certificate issuance, and request routing services."
    },
    {
      step: "2. Port & Firewall configuration setup",
      command: "sudo ufw allow 22/tcp && sudo ufw allow 80/tcp && sudo ufw allow 443/tcp && sudo ufw enable",
      notes: "Blocks unauthorized container port access, keeping database ports (3306) highly insulated from outer pings."
    },
    {
      step: "3. Docker configuration setup",
      command: "cat << 'EOF' > docker-compose.yml\\n(\\n...see Production Deployment panel inside Blueprint Suite for full docker-compose context...\\n)\\nEOF",
      notes: "Sets up isolated front-end, back-end servers, and database containers with health check triggers and shared memory limits."
    },
    {
      step: "4. Build and trigger Docker containers",
      command: "docker-compose up -d --build",
      notes: "Compiles Node production bundler and deploys secure web instances on network daemon safely."
    },
    {
      step: "5. SSL certificates request",
      command: "sudo certbot certonly --standalone -d crm.alpastrology.com",
      notes: "Issues valid HTTPS cryptographic certificate credentials."
    },
    {
      step: "6. Deploy Nginx configs and enable proxying",
      command: "sudo systemctl restart nginx",
      notes: "Reroutes all external traffic strictly through HTTPS securely onto Node server running inside docker networks."
    }
  ]
};
