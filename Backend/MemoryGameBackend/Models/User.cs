namespace MemoryGameBackend.Models;

public class User
{
  public int Id { get; set; }
  public string Username { get; set; } = string.Empty;
  public string Password { get; set; } = string.Empty; // For demo only; store hashed password in production
  public string UserType { get; set; } = "player";

  public ICollection<Score> Scores { get; set; } = new List<Score>();
}

