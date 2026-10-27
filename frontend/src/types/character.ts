export type SensitiveField<T> = {
  visible: boolean
  value: T | null
}

export type CharacterResponse = {
  id: number
  name: string

  gender: string | null
  nationality: string | null
  occupation: string | null
  identity: string | null

  characterType: "PLAYER_CHARACTER" | "NPC"
  ownerMemberId: number | null

  level: number | null
  rank: number | null

  str: number | null
  dex: number | null
  con: number | null
  intel: number | null
  app: number | null
  edu: number | null

  currentHp: number | null
  maxHp: number | null

  cashCents: SensitiveField<number>
  debtCents: SensitiveField<number>
  incomeCents: SensitiveField<number>
  expenseCents: SensitiveField<number>

  desire: SensitiveField<string>
  fear: SensitiveField<string>
  secret: SensitiveField<string>
}