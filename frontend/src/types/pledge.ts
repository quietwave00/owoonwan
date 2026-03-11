export type PledgeResponse = {
  uid: string;
  nickname: string;
  text: string;
  updatedAt: string | null;
  version: number;
  mine: boolean;
};

export type PledgeUpdateRequest = {
  text: string;
};
