import client from './client';
import type {
  RoadmapResponse,
  RoadmapGenerationRequest,
  StepStatusUpdateDTO,
  StepDTO,
} from '@/types';

export const roadmapApi = {
  getAll: async (): Promise<RoadmapResponse[]> => {
    const response = await client.get('/roadmap');
    return response.data;
  },

  getById: async (roadmapId: number): Promise<RoadmapResponse> => {
    const response = await client.get(`/roadmap/${roadmapId}`);
    return response.data;
  },

  generate: async (data: RoadmapGenerationRequest): Promise<RoadmapResponse> => {
    const response = await client.post('/roadmap/generate', data);
    return response.data;
  },

  getStep: async (roadmapId: number, stepId: number): Promise<StepDTO> => {
    const response = await client.get(`/roadmap/${roadmapId}/steps/${stepId}`);
    return response.data;
  },

  updateStepStatus: async (
    stepId: string,
    newStatus: StepStatusUpdateDTO['newStatus']
  ): Promise<RoadmapResponse> => {
    const response = await client.patch(`/roadmap/steps/${stepId}/status`, { newStatus });
    return response.data;
  },

  deleteRoadmap: async (roadmapId: number): Promise<void> => {
    await client.delete(`/roadmap/${roadmapId}`);
  },

  getRoadmapProgress: async (roadmapId: number): Promise<{ progressPercentage: number }> => {
    const response = await client.get(`/roadmap/${roadmapId}/progress`);
    return response.data;
  },

  regenerate: async (roadmapId: number): Promise<RoadmapResponse> => {
    const response = await client.post(`/roadmap/${roadmapId}/regenerate`);
    return response.data;
  },
};
