

-- insert admin user;
INSERT INTO oskari_users (user_name, first_name, last_name, email, uuid) VALUES('tuomas.riihimaki@ubigu.fi', 'Tuomas', 'Riihimäki', 'tuomas.riihimaki@ubigu.fi', gen_random_uuid ());
INSERT INTO oskari_users (user_name, first_name, last_name, email, uuid) VALUES('janne.heikkila@ubigu.fi', 'Janne', 'Heikkila', 'janne.heikkila@ubigu.fi', gen_random_uuid ());
INSERT INTO oskari_users (user_name, first_name, last_name, email, uuid) VALUES('ossi.tammi@ubigu.fi', 'Ossi', 'Tammi', 'ossi.tammi@ubigu.fi', gen_random_uuid ());

INSERT INTO oskari_users_roles (user_id, role_id) VALUES((SELECT id FROM oskari_users WHERE user_name = 'tuomas.riihimaki@ubigu.fi'), (SELECT id FROM oskari_roles WHERE name = 'Admin'));
INSERT INTO oskari_users_roles (user_id, role_id) VALUES((SELECT id FROM oskari_users WHERE user_name = 'janne.heikkila@ubigu.fi'), (SELECT id FROM oskari_roles WHERE name = 'Admin'));
INSERT INTO oskari_users_roles (user_id, role_id) VALUES((SELECT id FROM oskari_users WHERE user_name = 'ossi.tammi@ubigu.fi'), (SELECT id FROM oskari_roles WHERE name = 'Admin'));
