export interface FriendshipDTO {
    id: number;
    friendId: number;
    friendUsername: string;
    avatarIcon: string;
    activeTitle: string;
    status: string;
    createdAt: string;
    mmr: number;
    tier: string;
}

export interface ChatMessageDTO {
    senderUsername: string;
    receiverUsername: string;
    content: string;
    timestamp?: string;
}

export interface ChallengeDTO {
    senderUsername: string;
    receiverUsername: string;
    lobbyId: string;
}

export interface PublicProfileDTO {
    username: string;
    avatarIcon: string;
    description: string;
    activeTitle: string;
    selectedMedals: string;
    level: number;
    mmr: number;
    statistics: any;
    unlockedTitles: string[];
    showcase: any[];
    showcasedDeck: any;
    advancedStats: any;
}
