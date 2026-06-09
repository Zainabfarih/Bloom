// ============================================
// AUTH SERVICE DTOs
// ============================================

export interface UserDTO {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  role: 'STUDENT' | 'ADMIN' | 'USER';
  // Admin-enriched fields (present when fetched via admin endpoint)
  createdAt?: string;
  enabled?: boolean;
  locked?: boolean;
  deleted?: boolean;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  user: UserDTO;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
}

export interface PasswordResetRequest {
  email: string;
}

export interface PasswordUpdateRequest {
  token: string;
  newPassword: string;
}

export interface RefreshTokenRequest {
  refreshToken: string;
}

export interface TokenValidationResponse {
  valid: boolean;
  userId?: string;
  email?: string;
  role?: string;
}

export interface ErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  validationErrors?: Record<string, string>;
}

// ============================================
// CV SERVICE DTOs
// ============================================

export interface SkillsDTO {
  userId: number;
  cvUuid: string;
  skills: string[];
}

export interface CvResponse {
  uuid: string;
  title: string;
  source: 'UPLOAD' | 'MANUAL';
  originalFilename?: string;
  skills: string[];
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

/** Alias used by CvPage */
export type CvDTO = CvResponse;

export interface CvAnalysisIssue {
  type: string;
  severity: 'LOW' | 'MEDIUM' | 'HIGH';
  message: string;
  suggestion?: string;
}

export interface CvAnalysisResponse {
  cvUuid: string;
  atsScore: number;
  summary: string;
  strengths: string[];
  issues: CvAnalysisIssue[];
}

export interface ManualCvRequest {
  title: string;
  summary: string;
  experiences: string[];
  educations: string[];
  skills: string[];
  pdfBase64: string;
}

// ============================================
// JOB SERVICE DTOs
// ============================================

export interface ApplyOption {
  title?: string;
  link?: string;
}

export interface JobResult {
  job_id?: string;
  job_title?: string;
  job_company?: string;
  job_employment_type?: string;
  job_apply_link?: string;
  job_description?: string;
  job_posted_at_datetime_utc?: string;
  job_country?: string;
  job_country_code?: string;
  job_state?: string;
  job_city?: string;
  job_salary_currency?: string;
  job_salary_period?: string;
  job_salary_min?: number;
  job_salary_max?: number;
  job_salary_rawtext?: string;
  job_type?: string;
  job_is_remote?: boolean;
  job_required_skills?: string[];
  job_benefits?: string[];
  job_google_link?: string;
  job_offer_expiration_datetime_utc?: string;
  job_posting_language?: string;
  job_offer_expiration_timestamp?: number;
  job_posted_at_timestamp?: number;
  job_search_engine_link?: string;
  apply_options?: ApplyOption[];
}

export interface JobResponse {
  jobs_results: JobResult[];
  error?: string;
}

export interface JobDetailResponse {
  jobId: string;
  title: string;
  companyName: string;
  location: string;
  description: string;
  extensions?: string[];
  applyOptions?: ApplyOption[];
  extractedSkills: string[];
  fromSkillCache: boolean;
  fromSearchCache: boolean;
}

export interface SavedJobResponse {
  /** Numeric DB id — required as `targetJobId` when generating a roadmap. */
  id: number;
  uuid: string;
  jobExternalId: string;
  jobTitle: string;
  jobCompany: string;
  jobLocation: string;
  jobApplyUrl?: string;
  cvUuid?: string;
  requiredSkills: string[];
  matchedSkills: string[];
  missingSkills: string[];
  compatibilityScore: number;
  savedAt: string;
}

export interface SaveJobRequest {
  jobExternalId: string;
  jobTitle: string;
  jobCompany?: string;
  jobLocation?: string;
  jobApplyUrl?: string;
  requiredSkills?: string[];
  cvUuid?: string;
}

export interface JobSearchResult {
  jobId: string;
  title: string;
  companyName?: string;
  location?: string;
  extensions?: string[];
  applyOptions?: ApplyOption[];
}

export interface JobSearchResponse {
  jobs: JobSearchResult[];
  fromCache: boolean;
  totalResults: number;
}

export interface SkillGapResponse {
  userId: number;
  jobId: number;
  jobTitle: string;
  missingSkills: string[];
  matchingSkills?: string[];   // present if backend provides
  matchScore?: number;          // present if backend provides
}

// ============================================
// ROADMAP SERVICE DTOs
// ============================================

/**
 * Mirrors the backend `StepStatus` enum exactly.
 * Users cycle PENDING → IN_PROGRESS → COMPLETED; ACCEPTED/REJECTED are
 * set by the AI-review flow on the backend.
 */
export type StepStatus =
  | 'PENDING'
  | 'IN_PROGRESS'
  | 'COMPLETED'
  | 'ACCEPTED'
  | 'REJECTED';

export type ResourceType = 'COURSE' | 'TUTORIAL' | 'ARTICLE' | 'BOOK' | 'PROJECT';

export interface ResourceDTO {
  id: number;
  title: string;
  url: string;
  type: ResourceType;
}

export interface StepDTO {
  id: number;
  title: string;
  description: string;
  orderIndex: number;
  status: StepStatus;
  estimatedDuration: string;
  resources: ResourceDTO[];
}

export interface RoadmapResponse {
  id: number;
  targetJobId: number;
  targetJobTitle: string;
  progressPercentage: number;
  steps: StepDTO[];
}

export interface StepStatusUpdateDTO {
  newStatus: StepStatus;
}

export interface RoadmapSkillGapResponse {
  userId: number;
  jobId: number;
  jobTitle: string;
  missingSkills: string[];
}

export interface RoadmapGenerationRequest {
  targetJobId: number;
}

// ============================================
// ADMIN DTOs (mirrors auth-service AdminStatsResponse)
// ============================================

export interface DailyCount {
  date: string; // ISO yyyy-MM-dd
  count: number;
}

export interface AdminStatsResponse {
  totalUsers: number;
  activeUsers: number;
  deletedUsers: number;
  newUsersThisMonth: number;
  adminCount: number;
  studentCount: number;
  signupsByDay: DailyCount[];
}

// ============================================
// API RESPONSE WRAPPER
// ============================================

export interface ApiResponse<T> {
  success: boolean;
  data?: T;
  error?: ErrorResponse;
  message?: string;
}

export interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  currentPage: number;
  pageSize: number;
}
