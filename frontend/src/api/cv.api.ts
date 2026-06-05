import client from './client';
import type {
  CvResponse,
  CvAnalysisResponse,
  ManualCvRequest,
  SkillsDTO,
} from '@/types';

/** Skill item shape returned by the skills endpoint */
export interface SkillItem {
  name: string;
  confidence?: number;
}

/**
 * CV service client.
 * Backend base path: /api/cv  (frontend axios baseURL is /api → we use /cv/...)
 *
 * Endpoints (see CvController):
 *   POST   /cv/upload            (multipart: file, title?)
 *   POST   /cv/manual
 *   GET    /cv/me                (active CV)
 *   GET    /cv/me/all            (all CVs)
 *   GET    /cv/{uuid}/skills
 *   GET    /cv/{uuid}/analysis
 *   DELETE /cv/{uuid}
 */
export const cvApi = {
  /** All CVs for the current user. */
  getAllMyCvs: async (): Promise<CvResponse[]> => {
    const response = await client.get('/cv/me/all');
    return response.data;
  },

  /** The user's currently-active CV (404 if none — callers should handle). */
  getActiveCv: async (): Promise<CvResponse> => {
    const response = await client.get('/cv/me');
    return response.data;
  },

  upload: async (file: File, title?: string): Promise<CvResponse> => {
    const formData = new FormData();
    formData.append('file', file);
    if (title) formData.append('title', title);
    const response = await client.post('/cv/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return response.data;
  },

  createManualCv: async (data: ManualCvRequest): Promise<CvResponse> => {
    const response = await client.post('/cv/manual', data);
    return response.data;
  },

  /** ATS analysis (computed on the fly, not persisted). */
  getCvAnalysis: async (cvUuid: string): Promise<CvAnalysisResponse> => {
    const response = await client.get(`/cv/${cvUuid}/analysis`);
    return response.data;
  },

  deleteCv: async (cvUuid: string): Promise<void> => {
    await client.delete(`/cv/${cvUuid}`);
  },

  /**
   * Get skills for a CV. The backend returns SkillsDTO { skills: string[] };
   * we normalise to SkillItem[] for the UI.
   */
  getCvSkills: async (cvUuid: string): Promise<SkillItem[]> => {
    const response = await client.get(`/cv/${cvUuid}/skills`);
    const data: SkillsDTO = response.data;
    return (data.skills ?? []).map((name) => ({ name }));
  },
};
