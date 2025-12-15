using System.Text;
using MemoryGameBackend.Data;
using MemoryGameBackend.Models;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.EntityFrameworkCore;
using Microsoft.IdentityModel.Tokens;

var builder = WebApplication.CreateBuilder(args);

// Add services to the container.
builder.Services.AddControllers();
builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen();

// Database (MySQL with Pomelo)
var connectionString = builder.Configuration.GetConnectionString("DefaultConnection");
var useInMemory = builder.Environment.IsDevelopment() &&
  (string.IsNullOrWhiteSpace(connectionString) ||
   connectionString.Contains("YOUR_PASSWORD_HERE", StringComparison.OrdinalIgnoreCase));
builder.Services.AddDbContext<GameDbContext>(options =>
{
  if (useInMemory)
  {
    options.UseInMemoryDatabase("GameDb");
  }
  else
  {
    options.UseMySql(connectionString, ServerVersion.AutoDetect(connectionString));
  }
});

// JWT Authentication
var jwtKey = builder.Configuration["Jwt:Key"];
var jwtIssuer = builder.Configuration["Jwt:Issuer"];
var jwtAudience = builder.Configuration["Jwt:Audience"];

if (string.IsNullOrWhiteSpace(jwtKey))
{
  throw new InvalidOperationException("JWT Key is not configured. Please set Jwt:Key in appsettings.json.");
}

var signingKey = new SymmetricSecurityKey(Encoding.UTF8.GetBytes(jwtKey));

builder.Services
  .AddAuthentication(options =>
  {
    options.DefaultAuthenticateScheme = JwtBearerDefaults.AuthenticationScheme;
    options.DefaultChallengeScheme = JwtBearerDefaults.AuthenticationScheme;
  })
  .AddJwtBearer(options =>
  {
    options.RequireHttpsMetadata = false;
    options.SaveToken = true;
    options.TokenValidationParameters = new TokenValidationParameters
    {
      ValidateIssuer = !string.IsNullOrWhiteSpace(jwtIssuer),
      ValidateAudience = !string.IsNullOrWhiteSpace(jwtAudience),
      ValidateIssuerSigningKey = true,
      ValidIssuer = jwtIssuer,
      ValidAudience = jwtAudience,
      IssuerSigningKey = signingKey,
      ClockSkew = TimeSpan.Zero
    };
  });

builder.Services.AddAuthorization();

var app = builder.Build();

// Configure the HTTP request pipeline.
if (app.Environment.IsDevelopment())
{
  app.UseSwagger();
  app.UseSwaggerUI();
}

//app.UseHttpsRedirection();

app.UseAuthentication();
app.UseAuthorization();

// Dev seeding for InMemory provider
using (var scope = app.Services.CreateScope())
{
  var db = scope.ServiceProvider.GetRequiredService<GameDbContext>();
  if (useInMemory)
  {
    db.Database.EnsureCreated();
    if (!db.Users.Any())
    {
      db.Users.AddRange(
        new User { Username = "admin", Password = "admin", UserType = "Paid User" },
        new User { Username = "guest", Password = "guest", UserType = "Free User" }
      );
      db.SaveChanges();
    }
  }
}

app.MapControllers();

app.Run();

