import client from './client';
import type {
  JobDetailResponse,
  JobSearchResponse,
  SavedJobResponse,
  SaveJobRequest,
  SkillGapResponse,
} from '@/types';

export const jobApi = {
  searchJobs: async (
    query: string,
    location?: string,
    pagination?: { page: number; limit: number }
  ): Promise<JobSearchResponse> => {
    const params: Record<string, string | number> = { query };
    if (location) params.location = location;
    if (pagination) {
      params.page = pagination.page;
      params.limit = pagination.limit;
    }
    const response = await client.get('/job/search', { params });
    return response.data;
  },

  getJobDetails: async (jobId: string): Promise<JobDetailResponse> => {
    const response = await client.get(`/job/${jobId}`);
    return response.data;
  },

  saveJob: async (data: SaveJobRequest): Promise<SavedJobResponse> => {
    const response = await client.post('/job/saved', data);
    return response.data;
  },

  getSavedJobs: async (): Promise<SavedJobResponse[]> => {
    const response = await client.get('/job/saved');
    return response.data;
  },

  getSavedJob: async (uuid: string): Promise<SavedJobResponse> => {
    const response = await client.get(`/job/saved/${uuid}`);
    return response.data;
  },

  deleteSavedJob: async (uuid: string): Promise<void> => {
    await client.delete(`/job/saved/${uuid}`);
  },

  /**
   * Get skill gap for a specific job vs the user's active CV.
   * Pass jobId=undefined to skip the request (returns null).
   */
  getSkillGap: async (
    jobId?: number | null,
    cvUuid?: string
  ): Promise<SkillGapResponse | null> => {
    if (!jobId) return null;
    const params: Record<string, string | number> = { jobId };
    if (cvUuid) params.cvUuid = cvUuid;
    const response = await client.get('/job/skill-gap', { params });
    return response.data;
  },

  getRecommendedJobs: async (cvUuid: string): Promise<JobSearchResponse> => {
    const response = await client.get(`/job/recommended`, { params: { cvUuid } });
    return response.data;
  },

  analyzeJobSkills: async (jobId: string): Promise<{ skills: string[] }> => {
    const response = await client.get(`/job/${jobId}/skills`);
    return response.data;
  },
};
