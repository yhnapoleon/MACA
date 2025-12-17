using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using MemoryGameBackend.Data;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace MemoryGameBackend.Controllers;

[ApiController]
[Route("api")]
public class ScoreController : ControllerBase
{
  private readonly GameDbContext _db;

  public ScoreController(GameDbContext db)
  {
    _db = db;
  }

  public record CreateScoreRequest(int Score);

  [Authorize]
  [HttpPost("scores")]
  public async Task<IActionResult> CreateScore([FromBody] CreateScoreRequest request, CancellationToken ct)
  {
    var userId = GetUserIdFromClaims(User);
    if (userId is null)
    {
      return Unauthorized(new { message = "Invalid token" });
    }

    var score = new Models.Score
    {
      UserId = userId.Value,
      ScoreValue = request.Score,
      CreatedAt = DateTime.UtcNow
    };

    _db.Scores.Add(score);
    await _db.SaveChangesAsync(ct);

    return CreatedAtAction(nameof(GetTop5), new { id = score.Id }, new { id = score.Id });
  }

  [HttpGet("leaderboard/top5")]
  public async Task<IActionResult> GetTop5(CancellationToken ct)
  {
    var top5 = await _db.Scores
      .AsNoTracking()
      // 导航属性在投影时会自动生成 LEFT JOIN，无需 Include
      .OrderBy(s => s.ScoreValue)
      .Take(5)
      .Select(s => new
      {
        user = s.User != null ? s.User.Username : "Unknown",
        score = s.ScoreValue
      })
      .ToListAsync(ct);

    // Android 客户端期望直接返回数组，而不是包在 { leaderboard: [...] } 中
    return Ok(top5);
  }

  private static int? GetUserIdFromClaims(ClaimsPrincipal user)
  {
    var idClaim = user.FindFirst(ClaimTypes.NameIdentifier)
      ?? user.FindFirst(JwtRegisteredClaimNames.Sub);

    return int.TryParse(idClaim?.Value, out var id) ? id : null;
  }
}

