using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using System.Text;
using MemoryGameBackend.Data;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using Microsoft.IdentityModel.Tokens;

namespace MemoryGameBackend.Controllers;

[ApiController]
[Route("api/auth")]
public class AuthController : ControllerBase
{
  private readonly GameDbContext _db;
  private readonly IConfiguration _config;

  public AuthController(GameDbContext db, IConfiguration config)
  {
    _db = db;
    _config = config;
  }

  public record LoginRequest(string Username, string Password);

  [HttpPost("login")]
  public async Task<IActionResult> Login([FromBody] LoginRequest request, CancellationToken ct)
  {
    if (string.IsNullOrWhiteSpace(request.Username) || string.IsNullOrWhiteSpace(request.Password))
    {
      return BadRequest(new { message = "Username and password are required." });
    }

    var user = await _db.Users
      .AsNoTracking()
      .FirstOrDefaultAsync(u => u.Username == request.Username, ct);

    if (user is null || user.Password != request.Password)
    {
      return Unauthorized(new { message = "Invalid username or password." });
    }

    var token = GenerateJwtForUser(user);

    return Ok(new
    {
      userType = user.UserType,
      token
    });
  }

  private string GenerateJwtForUser(Models.User user)
  {
    var key = _config["Jwt:Key"] ?? throw new InvalidOperationException("Jwt:Key not configured.");
    var issuer = _config["Jwt:Issuer"];
    var audience = _config["Jwt:Audience"];
    var expireMinutes = int.TryParse(_config["Jwt:ExpireMinutes"], out var m) ? m : 60;

    var claims = new List<Claim>
    {
      new(JwtRegisteredClaimNames.Sub, user.Id.ToString()),
      new(ClaimTypes.Name, user.Username),
      new(ClaimTypes.Role, user.UserType ?? string.Empty)
    };

    var signingKey = new SymmetricSecurityKey(Encoding.UTF8.GetBytes(key));
    var creds = new SigningCredentials(signingKey, SecurityAlgorithms.HmacSha256);

    var token = new JwtSecurityToken(
      issuer: issuer,
      audience: audience,
      claims: claims,
      expires: DateTime.UtcNow.AddMinutes(expireMinutes),
      signingCredentials: creds
    );

    return new JwtSecurityTokenHandler().WriteToken(token);
  }
}

