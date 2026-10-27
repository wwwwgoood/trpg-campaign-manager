export type MemoCategory="NPC"|"SECRET"| "LOCATION"
  | "ORGANIZATION"
  | "ITEM"
  | "EVENT"
  | "CUSTOM"
export interface Memo{
    id:number
    campaignId: number
    name: string
    category: MemoCategory
    description: string
}
export interface CreateMemoRequest{
  name: string
  category: MemoCategory
  description: string
}

export interface UpdateMemoRequest {
  name: string
  category: MemoCategory
  description: string
}