import client from './client';
import type {
  JobDetailResponse,
  JobSearchResponse,
  SavedJobResponse,
} from '@/types';

export const jobApi = {
  searchJobs: async (
    query: string,
    location?: string
  ): Promise<JobSearchResponse> => {
    const params: Record<string, string> = { query };
    if (location) params.location = location;
    const response = await client.get('/job/search', { params });
    return response.data;
  },

  getJobDetails: async (jobId: string): Promise<JobDetailResponse> => {
    const response = await client.get(`/job/${encodeURIComponent(jobId)}`);
    return response.data;
  },

  saveJob: async (jobId: string, cvUuid?: string): Promise<SavedJobResponse> => {
    const params: Record<string, string> = {};
    if (cvUuid) params.cvUuid = cvUuid;
    const response = await client.post(
      `/job/saved/${encodeURIComponent(jobId)}`,
      null,
      { params }
    );
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

  deleteSavedJob: async (jobExternalId: string): Promise<void> => {
    await client.delete(`/job/saved/${encodeURIComponent(jobExternalId)}`);
  },

  evictCache: async (query: string, location?: string): Promise<void> => {
    const params: Record<string, string> = { query };
    if (location) params.location = location;
    await client.delete('/job/admin/cache', { params });
  },
};
