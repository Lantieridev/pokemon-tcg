/**
 * Modelos TypeScript para la Campaña PvE (Modo Historia).
 * Reflejan los DTOs del backend: CampaignProgressResponseDTO y CampaignNodeDTO.
 */

/** Estado posible de un nodo de campaña */
export type CampaignNodeStatus = 'LOCKED' | 'UNLOCKED' | 'CLEARED';

/** Refleja CampaignNodeDTO.java */
export interface CampaignNode {
  id: number;
  name: string;
  botName: string;
  status: CampaignNodeStatus;
  rewardCoins: number;
  rewardXp: number;
}

/** Refleja CampaignProgressResponseDTO.java */
export interface CampaignProgressResponse {
  clearedNodesCount: number;
  totalNodesCount: number;
  nodes: CampaignNode[];
}

/** Respuesta del endpoint POST /api/campaign/challenge/{nodeId} */
export interface ChallengeResponse {
  matchId: string;
}
