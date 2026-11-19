INSERT INTO conducteur (id, nom, prenom, email, telephone, numero_permis) VALUES
                                                                              (1, 'Alaoui', 'Hamid', 'hamid.alaoui@mail.com', '0661000001', 'P-100001'),
                                                                              (2, 'Benali', 'Youssef', 'youssef.benali@mail.com', '0661000002', 'P-100002'),
                                                                              (3, 'Chraibi', 'Nadia', 'nadia.chraibi@mail.com', '0661000003', 'P-100003'),
                                                                              (4, 'Tazi', 'Omar', 'omar.tazi@mail.com', '0661000004', 'P-100004'),
                                                                              (5, 'Idrissi', 'Sara', 'sara.idrissi@mail.com', '0661000005', 'P-100005');

INSERT INTO passager (id, nom, prenom, email, telephone) VALUES
                                                             (1, 'Bennani', 'Salma', 'salma.bennani@mail.com', '0662000001'),
                                                             (2, 'Haddad', 'Karim', 'karim.haddad@mail.com', '0662000002'),
                                                             (3, 'Berrada', 'Imane', 'imane.berrada@mail.com', '0662000003'),
                                                             (4, 'Lahlou', 'Yassine', 'yassine.lahlou@mail.com', '0662000004'),
                                                             (5, 'Fassi', 'Meryem', 'meryem.fassi@mail.com', '0662000005');

INSERT INTO vehicule (id, marque, modele, immatriculation, couleur, capacite, conducteur_id) VALUES
                                                                                                 (1, 'Dacia', 'Logan', '12345-A-6', 'Blanc', 4, 1),
                                                                                                 (2, 'Renault', 'Clio', '23456-B-7', 'Gris', 3, 2),
                                                                                                 (3, 'Peugeot', '208', '34567-D-8', 'Noir', 4, 3),
                                                                                                 (4, 'Hyundai', 'i10', '45678-H-9', 'Rouge', 4, 4),
                                                                                                 (5, 'Toyota', 'Corolla', '56789-A-1', 'Bleu', 4, 5);

INSERT INTO trajet (id, vehicule_id, date_depart, places_proposees, statut) VALUES
                                                                                (1, 1, CURRENT_DATE + INTERVAL '5 days 17 hours', 3, 'PUBLIE'),
                                                                                (2, 2, CURRENT_DATE + INTERVAL '2 days 8 hours', 3, 'PUBLIE'),
                                                                                (3, 3, CURRENT_DATE + INTERVAL '3 days 9 hours', 3, 'PUBLIE'),
                                                                                (4, 4, CURRENT_DATE + INTERVAL '7 days 7 hours 30 minutes', 4, 'PUBLIE'),
                                                                                (5, 5, CURRENT_DATE + INTERVAL '4 days 14 hours', 3, 'PUBLIE');

INSERT INTO point_arret (id, trajet_id, ville, ordre, heure_passage, prix_depuis_depart) VALUES
-- Trajet 1 (Hamid) : Rabat -> Casablanca -> Settat -> Marrakech
(1, 1, 'Rabat', 0, CURRENT_DATE + INTERVAL '5 days 17 hours', 0),
(2, 1, 'Casablanca', 1, CURRENT_DATE + INTERVAL '5 days 18 hours 15 minutes', 40),
(3, 1, 'Settat', 2, CURRENT_DATE + INTERVAL '5 days 19 hours 15 minutes', 70),
(4, 1, 'Marrakech', 3, CURRENT_DATE + INTERVAL '5 days 21 hours 30 minutes', 120),
-- Trajet 2 : Casablanca -> Settat -> Marrakech
(5, 2, 'Casablanca', 0, CURRENT_DATE + INTERVAL '2 days 8 hours', 0),
(6, 2, 'Settat', 1, CURRENT_DATE + INTERVAL '2 days 9 hours', 30),
(7, 2, 'Marrakech', 2, CURRENT_DATE + INTERVAL '2 days 10 hours 30 minutes', 80),
-- Trajet 3 : Rabat -> Meknès -> Fès
(8, 3, 'Rabat', 0, CURRENT_DATE + INTERVAL '3 days 9 hours', 0),
(9, 3, 'Meknès', 1, CURRENT_DATE + INTERVAL '3 days 10 hours 15 minutes', 40),
(10, 3, 'Fès', 2, CURRENT_DATE + INTERVAL '3 days 11 hours', 70),
-- Trajet 4 : Tanger -> Rabat -> Casablanca
(11, 4, 'Tanger', 0, CURRENT_DATE + INTERVAL '7 days 7 hours 30 minutes', 0),
(12, 4, 'Rabat', 1, CURRENT_DATE + INTERVAL '7 days 10 hours', 70),
(13, 4, 'Casablanca', 2, CURRENT_DATE + INTERVAL '7 days 11 hours 30 minutes', 100),
-- Trajet 5 : Marrakech -> Agadir
(14, 5, 'Marrakech', 0, CURRENT_DATE + INTERVAL '4 days 14 hours', 0),
(15, 5, 'Agadir', 1, CURRENT_DATE + INTERVAL '4 days 17 hours 30 minutes', 90);

INSERT INTO reservation (id, passager_id, trajet_id, point_montee_id, point_descente_id, nombre_places, montant_total, date_reservation, statut) VALUES
                                                                                                                                                     (1, 1, 2, 5, 7, 1, 80.00, NOW(), 'CONFIRMEE'),
                                                                                                                                                     (2, 2, 3, 8, 10, 2, 140.00, NOW(), 'CONFIRMEE'),
                                                                                                                                                     (3, 3, 4, 11, 13, 1, 100.00, NOW(), 'CONFIRMEE'),
                                                                                                                                                     (4, 4, 5, 14, 15, 2, 180.00, NOW(), 'CONFIRMEE'),
                                                                                                                                                     (5, 5, 2, 6, 7, 1, 50.00, NOW(), 'CONFIRMEE'),
                                                                                                                                                     (6, 2, 4, 12, 13, 1, 30.00, NOW(), 'ANNULEE');

SELECT setval(pg_get_serial_sequence('conducteur', 'id'), (SELECT MAX(id) FROM conducteur));
SELECT setval(pg_get_serial_sequence('passager', 'id'), (SELECT MAX(id) FROM passager));
SELECT setval(pg_get_serial_sequence('vehicule', 'id'), (SELECT MAX(id) FROM vehicule));
SELECT setval(pg_get_serial_sequence('trajet', 'id'), (SELECT MAX(id) FROM trajet));
SELECT setval(pg_get_serial_sequence('point_arret', 'id'), (SELECT MAX(id) FROM point_arret));
SELECT setval(pg_get_serial_sequence('reservation', 'id'), (SELECT MAX(id) FROM reservation));