import { http, HttpResponse } from 'msw';

const API_BASE = '/api';

export const handlers = [
  // Auth handlers
  http.post(`${API_BASE}/auth/login`, async ({ request }) => {
    const body = (await request.json()) as { email: string; password: string };

    if (body.email === 'test@example.com' && body.password === 'password123') {
      return HttpResponse.json({
        accessToken: 'mock-access-token',
        refreshToken: 'mock-refresh-token',
        user: {
          id: 1,
          email: 'test@example.com',
          name: 'Test User',
        },
      });
    }

    return new HttpResponse(null, { status: 401 });
  }),

  http.post(`${API_BASE}/auth/register`, async ({ request }) => {
    const body = (await request.json()) as { email: string; password: string; name: string };

    return HttpResponse.json({
      accessToken: 'mock-access-token',
      refreshToken: 'mock-refresh-token',
      user: {
        id: 1,
        email: body.email,
        name: body.name,
      },
    });
  }),

  http.get(`${API_BASE}/auth/me`, () => {
    return HttpResponse.json({
      id: 1,
      email: 'test@example.com',
      name: 'Test User',
    });
  }),

  // Agent handlers
  http.get(`${API_BASE}/agents`, () => {
    return HttpResponse.json([
      {
        id: 1,
        name: 'Test Agent',
        slug: 'test-agent',
        description: 'A test agent',
        isActive: true,
      },
    ]);
  }),

  http.get(`${API_BASE}/agents/:id`, ({ params }) => {
    return HttpResponse.json({
      id: Number(params.id),
      name: 'Test Agent',
      slug: 'test-agent',
      description: 'A test agent',
      isActive: true,
    });
  }),

  // Health check
  http.get(`${API_BASE}/health`, () => {
    return HttpResponse.json({ status: 'UP' });
  }),
];
