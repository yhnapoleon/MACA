using Microsoft.AspNetCore.Mvc;

namespace MemoryGameBackend.Controllers;

[ApiController]
[Route("api")]
public class GameController : ControllerBase
{
  [HttpGet("game/images")]
  public IActionResult GetImages([FromQuery] int count = 20)
  {
    var safeCount = Math.Clamp(count, 1, 100);
    var rnd = new Random();
    var images = Enumerable.Range(1, safeCount)
      .Select(_ => $"https://picsum.photos/200/300?random={rnd.Next(1, int.MaxValue)}")
      .ToList();

    return Ok(new { images });
  }

  [HttpGet("ads/next")]
  public IActionResult GetNextAd()
  {
    // Placeholder ad image
    var adUrl = "https://picsum.photos/seed/ad/728/90";
    return Ok(new { adUrl });
  }
}

