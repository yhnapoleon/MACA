using MemoryGameBackend.Data;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace MemoryGameBackend.Controllers;

[ApiController]
[Route("api")]
public class AdsController : ControllerBase
{
  private readonly GameDbContext _db;
  private static readonly object _lastAdLock = new();
  private static string? _lastServedFileName;

  public AdsController(GameDbContext db)
  {
    _db = db;
  }

  [HttpGet("ads/next")]
  public async Task<IActionResult> GetNextAd([FromQuery] string? lastFileName, CancellationToken ct)
  {
    var count = await _db.Ads.CountAsync(ct);
    if (count == 0)
    {
      return NotFound(new { message = "No ads available" });
    }

    // 优先使用客户端传入的上一张（首轮换场景）；否则回退为服务端记录的上一张
    string? lastFileNameSnapshot = lastFileName;
    if (string.IsNullOrWhiteSpace(lastFileNameSnapshot))
    {
      lock (_lastAdLock)
      {
        lastFileNameSnapshot = _lastServedFileName;
      }
    }

    // 当存在至少两张图片时，排除上一张，确保不会与上一次相同
    IQueryable<Models.Ad> query = _db.Ads.AsNoTracking();
    if (!string.IsNullOrWhiteSpace(lastFileNameSnapshot) && count > 1)
    {
      query = query.Where(a => a.ImageFileName != lastFileNameSnapshot);
    }

    var eligibleCount = await query.CountAsync(ct);
    // 若排除后无可选项（例如库中仅有一张图或客户端传入的是唯一项），则回退到全量集合
    if (eligibleCount == 0)
    {
      query = _db.Ads.AsNoTracking();
      eligibleCount = await query.CountAsync(ct);
    }

    var index = Random.Shared.Next(eligibleCount);
    var fileName = await query
      .OrderBy(a => a.Id)
      .Skip(index)
      .Select(a => a.ImageFileName)
      .FirstAsync(ct);

    if (string.IsNullOrWhiteSpace(fileName))
    {
      return NotFound(new { message = "No ads available" });
    }

    lock (_lastAdLock)
    {
      _lastServedFileName = fileName;
    }

    const string baseUrl = "http://10.0.2.2:5180";
    var adUrl = $"{baseUrl}/ad-images/{fileName}";
    return Ok(new { adUrl });
  }
}


