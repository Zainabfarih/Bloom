import client from './client';
import type {
  CvResponse,
  CvAnalysisResponse,
  ManualCvRequest,
  SkillsDTO,
} from '@/types';

export interface SkillItem {
  name: string;
  confidence?: number;
}

export const cvApi = {
  getAllMyCvs: async (): Promise<CvResponse[]> => {
    const response = await client.get('/cv/me/all');
    return response.data;
  },

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

  getCvAnalysis: async (cvUuid: string): Promise<CvAnalysisResponse> => {
    const response = await client.get(`/cv/${cvUuid}/analysis`);
    return response.data;
  },

  deleteCv: async (cvUuid: string): Promise<void> => {
    await client.delete(`/cv/${cvUuid}`);
  },

  getCvSkills: async (cvUuid: string): Promise<SkillItem[]> => {
    const response = await client.get(`/cv/${cvUuid}/skills`);
    const data: SkillsDTO = response.data;
    return (data.skills ?? []).map((name) => ({ name }));
  },

  downloadCv: async (cvUuid: string): Promise<Blob> => {
    const response = await client.get(`/cv/${cvUuid}/file`, {
      responseType: 'blob',
    });
    return response.data;
  },
};
