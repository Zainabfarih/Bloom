import client from './client';
import type {
  JobDetailResponse,
  JobSearchResponse,
  SavedJobResponse,
} from '@/types';

/**
 * Job service client.
 * Backend base path: /api/job (see JobController).
 *
 *   GET    /job/search?query&location   (SerpAPI, cached 24h)
 *   GET    /job/{jobId}                 (detail — requires a prior search)
 *   POST   /job/saved/{jobId}?cvUuid    (save + server-side skill match)
 *   GET    /job/saved                   (ordered by compatibility DESC)
 *   GET    /job/saved/{uuid}
 *   DELETE /job/saved/{jobExternalId}   (NB: deletes by EXTERNAL id, not uuid)
 */
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

  // Match against the active CV (or cvUuid) and persist the result.
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

  /**
   * Remove a saved job. The backend deletes by the job's EXTERNAL id
   * (the SerpAPI id), NOT the saved-job uuid.
   */
  deleteSavedJob: async (jobExternalId: string): Promise<void> => {
    await client.delete(`/job/saved/${encodeURIComponent(jobExternalId)}`);
  },
};
