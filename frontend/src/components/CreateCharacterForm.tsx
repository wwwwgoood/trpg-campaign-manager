import { useState } from "react"

type Props = {
  campaignId: number
  ownerMemberId: number
  onCreated: () => void
}

export default function CreateCharacterForm({
  campaignId,
  ownerMemberId,
  onCreated
}: Props) {

  const [name, setName] = useState("")
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function handleSubmit(event: React.FormEvent) {
    event.preventDefault()

    setLoading(true)
    setError(null)

    try {
      const response = await fetch(
        `http://localhost:8080/api/campaigns/${campaignId}/player-characters`,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json"
          },
          body: JSON.stringify({
            name,
            gender: "Unknown",
            nationality: "Unknown",
            occupation: "Unknown",
            identity: "Unknown",

            level: 1,
            rank: 1,

            str: 50,
            dex: 50,
            con: 50,
            intel: 50,
            app: 50,
            edu: 50,

            currentHp: 10,

            cashCents: 0,
            debtCents: 0,
            incomeCents: 0,
            expenseCents: 0,

            desire: "Unknown",
            fear: "Unknown",
            secret: "Unknown",

            ownerMemberId,

            skills: [
              {
                skillType: "PERSUASION",
                value: 50,
                unknown: false
              }
            ],

            featIds: [],
            items: [],
            affinities: []
          })
        }
      )

      if (!response.ok) {
        throw new Error("Failed to create character")
      }

      setName("")
      onCreated()

    } catch (error) {
      if (error instanceof Error) {
        setError(error.message)
      }
    } finally {
      setLoading(false)
    }
  }

  return (
    <form onSubmit={handleSubmit}>
      <h2>Create Character</h2>

      <input
        value={name}
        onChange={event => setName(event.target.value)}
        placeholder="Character name"
      />

      <button
        type="submit"
        disabled={loading}
      >
        {loading ? "Creating..." : "Create"}
      </button>

      {error && <p>{error}</p>}
    </form>
  )
}