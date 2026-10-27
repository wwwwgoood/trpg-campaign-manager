import type { Memo, UpdateMemoRequest,CreateMemoRequest } from "../types/memo";

export async function updateMemo(
  memoId: number,
  request: UpdateMemoRequest
): Promise<Memo> {
  const response = await fetch(
    `http://localhost:8080/api/memos/${memoId}`,
    {
      method: "PUT",
      headers: {
        "Content-Type": "application/json"
      },
      body: JSON.stringify(request)
    }
  )

  if (!response.ok) {
    throw new Error(`修改Memo失败：${response.status}`)
  }

  return await response.json()
}
export async function getMemos(campaignId: number): Promise<Memo[]> {
    const response = await fetch(`http://localhost:8080/api/campaigns/${campaignId}/memos`);
    if (!response.ok) {
        throw new Error(`获取Memo失败：${response.status}`)
    }
    const data: Memo[] = await response.json();
    return data;
}

export async function createMemo(campaignId: number,
    request: CreateMemoRequest): Promise<Memo> {
    const response = await fetch(`http://localhost:8080/api/campaigns/${campaignId}/memos`,
        {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(request)
        }
    )
    if (!response.ok) {
        throw new Error(`创建Memo失败：${response.status}`)
    }

    const data: Memo = await response.json()
    return data
}

export async function deleteMemo(memoId: number): Promise<void> {
  const response = await fetch(
    `http://localhost:8080/api/memos/${memoId}`,
    {
      method: "DELETE"
    }
  )

  if (!response.ok) {
    throw new Error(`删除Memo失败：${response.status}`)
  }
}