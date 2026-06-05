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

export const cvApi = {
  getAllCvs: async (): Promise<CvResponse[]> => {
    const response = await client.get('/cv');
    return response.data;
  },

  /** Alias — used throughout the app */
  getAllMyCvs: async (): Promise<CvResponse[]> => {
    const response = await client.get('/cv');
    return response.data;
  },

  getCvByUuid: async (uuid: string): Promise<CvResponse> => {
    const response = await client.get(`/cv/${uuid}`);
    return response.data;
  },

  upload: async (file: File): Promise<CvResponse> => {
    const formData = new FormData();
    formData.append('file', file);
    const response = await client.post('/cv/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return response.data;
  },

  createManualCv: async (data: ManualCvRequest): Promise<CvResponse> => {
    const response = await client.post('/cv/manual', data);
    return response.data;
  },

  analyzeCv: async (cvUuid: string): Promise<CvAnalysisResponse> => {
    const response = await client.get(`/cv/${cvUuid}/analyze`);
    return response.data;
  },

  /** Alias used by CvPage */
  getCvAnalysis: async (cvUuid: string): Promise<CvAnalysisResponse> => {
    const response = await client.get(`/cv/${cvUuid}/analyze`);
    return response.data;
  },

  setActiveCv: async (cvUuid: string): Promise<CvResponse> => {
    const response = await client.patch(`/cv/${cvUuid}/activate`);
    return response.data;
  },

  deleteCv: async (cvUuid: string): Promise<void> => {
    await client.delete(`/cv/${cvUuid}`);
  },

  updateCvSkills: async (cvUuid: string, skills: string[]): Promise<SkillsDTO> => {
    const response = await client.put(`/cv/${cvUuid}/skills`, { skills });
    return response.data;
  },

  /**
   * Get skills for a CV.
   * The backend returns SkillsDTO { skills: string[] };
   * we normalise to SkillItem[] for the UI.
   */
  getCvSkills: async (cvUuid: string): Promise<SkillItem[]> => {
    const response = await client.get(`/cv/${cvUuid}/skills`);
    const data: SkillsDTO = response.data;
    return (data.skills ?? []).map((name) => ({ name }));
  },
};
