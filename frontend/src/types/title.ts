export type TitleScopeResponse = {
  human: boolean;
  soul: boolean;
  count: number;
};

export type TitleResponse = {
  uid: string;
  weekKey: string;
  monthKey: string;
  weekly: TitleScopeResponse;
  monthly: TitleScopeResponse;
  special: {
    kakkdugi: boolean;
  };
  effectiveBadges: string[];
};

export type AdminTitleVerificationResponse = {
  weekKey: string;
  monthKey: string;
  titles: TitleResponse[];
};
