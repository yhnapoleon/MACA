using MemoryGameBackend.Data;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace MemoryGameBackend.Controllers;

[ApiController]
[Route("api")]
public class AdsController : ControllerBase
{
  private readonly GameDbContext _db;

  public AdsController(GameDbContext db)
  {
    _db = db;
  }

  [HttpGet("ads/next")]
  public async Task<IActionResult> GetNextAd(CancellationToken ct)
  {
    var count = await _db.Ads.CountAsync(ct);
    if (count == 0)
    {
      return NotFound(new { message = "No ads available" });
    }

    var index = Random.Shared.Next(count);
    var fileName = await _db.Ads
      .AsNoTracking()
      .OrderBy(a => a.Id)
      .Skip(index)
      .Select(a => a.ImageFileName)
      .FirstAsync(ct);

    if (string.IsNullOrWhiteSpace(fileName))
    {
      return NotFound(new { message = "No ads available" });
    }

    const string baseUrl = "http://10.0.2.2:5180";
    var adUrl = $"{baseUrl}/ad-images/{fileName}";
    return Ok(new { adUrl });
  }
}


