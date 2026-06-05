import client from './client';
import type {
  RoadmapResponse,
  RoadmapGenerationRequest,
  StepStatus,
} from '@/types';

/**
 * Roadmap service client.
 * Backend base path: /api/roadmap (see RoadmapController).
 *
 *   POST  /roadmap/generate              { targetJobId }   (targetJobId = SavedJob.id)
 *   GET   /roadmap                       (all roadmaps for user)
 *   GET   /roadmap/{roadmapId}
 *   PATCH /roadmap/steps/{stepId}/status { newStatus }
 */
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

  /** Returns the full updated roadmap (with recomputed progress). */
  updateStepStatus: async (
    stepId: number,
    newStatus: StepStatus
  ): Promise<RoadmapResponse> => {
    const response = await client.patch(`/roadmap/steps/${stepId}/status`, {
      newStatus,
    });
    return response.data;
  },
};
