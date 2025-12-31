export interface Occupation {
  occCode: string;
  occName: string;
  description?: string;
}

export interface OccupationRequest {
  name: string;
  description?: string;
}

export interface OccupationResponse {
  id: number;
  name: string;
  description: string;
  employeeCount: number;
  createdAt: string;
  updatedAt: string;
}
