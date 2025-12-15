namespace MemoryGameBackend.Models;

public class Score
{
  public int Id { get; set; }
  public int UserId { get; set; }
  public int ScoreValue { get; set; }
  public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

  public User? User { get; set; }
}

