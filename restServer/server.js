var express = require('express'),
	mongoose = require('mongoose');
	models = require('./models.js'),
	Session = require('connect-mongodb'),
	check = require('validator').check,
	https = require('https'),
	http = require('http'),
	fs = require('fs'),
	User = models.User,
	Game = models.Game,
	LoginToken = models.LoginToken;

mongoose.connect('localhost', 'dunk', function(err){
	if(err) throw err;
});

var options = {
  key: fs.readFileSync('/etc/ssl/self-signed/server.key'),
  cert: fs.readFileSync('/etc/ssl/self-signed/server.crt')
};

var db = mongoose.connection;
db.on('error', console.error.bind(console, 'connection error:'));

db.once('open', function callback() {

	var app = express();
	app.use(express.cookieParser());
	app.use(express.bodyParser());

	var session = express.session({store: new Session({db: mongoose.connection.db, maxAge: 300000}), secret: 's!p@r#o$S%^r&t' })
  	app.use(session);

	function authenticateFromLoginToken(req, res, next) {
		var cookie = JSON.parse(req.cookies.logintoken);

		LoginToken.findOne({ userId: cookie.email,
							series: cookie.series,
							token: cookie.token }, (function(err, token) {
			if (!token) {
				res.send(401,'Not logged in.');
				return;
			}

			User.findOne({ userId: token.userId }, function(err, user) {
				if (user) {
						req.session.user_id = user.id;
						req.currentUser = user;

						token.token = token.randomToken();
						token.save(function() {
							res.cookie('logintoken', token.cookieValue, { expires: new Date(Date.now() + 2 * 604800000), path: '/' });
							next();
						});
				} else {
					res.send(401, 'Not logged in.');
				}
			});
		}));
	}

  	function loadUser(req, res, next) {
		if (req.session.userId) {
				User.findOne({'userId': req.session.userId}, function(err, user) {
			  		if (user) {
				    	req.currentUser = user;
				    	next();
				  	} else {
				    	res.send(401,'Not logged in.');
				  	}
				});
		} else if (req.cookies.logintoken) {
			authenticateFromLoginToken(req, res, next);
		} else {
			res.send(401, 'Not logged in.');
		}
	}

	app.post('/login', function(req, res) {
		if (req.session.userId)
		{
			res.send(500, 'Already logged in.');
		}
		else {
			if ((req.body.email || req.body.cardNum) && req.body.password) {
				var searchVal = req.body.email ? req.body.email : req.body.cardNum;
				User.findOne( {$or:[{email: searchVal}, {cardNum: searchVal}]}, function(err, user) {
					if (user && user.authenticate(req.body.password)) {
						req.session.userId = user.userId;

						// Remember me
						if (req.body.remember_me) {
							var loginToken = new LoginToken({ userId: user.userId });
							loginToken.save(function() {
								res.cookie('logintoken', loginToken.cookieValue, { expires: new Date(Date.now() + 2 * 604800000), path: '/' });
								res.send(200,'Logged in.');
							});
						} else {
							res.send(200,'Logged in.');
						}

					} else {
						res.send(401,'Invalid email/cardNum or password.');
					}
				});
			} else {
				res.send(500, 'Login reqiures email|cardNum & password');
			}
		}
	});

	app.get('/logout', loadUser, function(req, res) {
		if (req.session) {
			LoginToken.remove({ userId: req.currentUser.userId }, function() {});
    		res.clearCookie('logintoken');
    		req.session.destroy(function() {});
    		res.json({success:true});
		}
		else
			res.json({success:false});
	});

	app.post('/users', function(req, res) {
		if (req.session.userId) {
			res.send(500, 'Already logged in. No need to register');
		}
		else {
			// Require exisiting cardNum, pin, email, and password
			if (req.body.cardNum && req.body.pin && req.body.email && req.body.password) {
				User.findOne({cardNum: req.body.cardNum}, function(err, user) {
					if (user && !user.registered) {
						if (req.body.pin == user.pin) {
							if (check(req.body.email).isEmail()) {
								user.userId = req.body.email.match(/[^@]+/)[0];
								user.password = req.body.password;
								user.email = req.body.email;
								user.registered = true;
								user.save(function(error) {
									if (error) {
										res.json({success: false, message: error.err})
									}
									else
										res.json({success: true})
								});
							}
							else {
								res.json({success: false, message: 'Invalid email.'});
							}
						}
						else {
							res.json({success: false, message: 'Invalid pin.'});
						}						
						
					} else {
						if (!user)
							res.json({success: false, message: 'No user found with that card number'});
						else 
							res.json({success: false, message: 'User already registered'});
					}
				});
			}
			else {
				res.json({success: false, message: 'Missing arguments. Need cardNum, pin, desired email, and desired password'});
			}
		}
	});

	app.get('/users', function(req, res) {
		User.find(function(err, users) {
			if (err)
			{
				console.log(err);
			}
			else
			{
				res.json(users);
			}
		});
	});

	app.get('/user', loadUser, function(req, res) {
		res.json(req.currentUser);
	});

	// Games
	app.get('/games', function(req, res) {
		Game.find(function(err, games) {
			if (err)
			{
				console.log(err);
			}
			else
			{
				res.json(games);
			}
		});
	});

	app.get('/game/:id', loadUser, function(req, res) {
		Game.findOne({_id: mongoose.Types.ObjectId(req.params.id)}, function(err, game) {
			if (err)
			{
				console.log(err);
				res.json(err);

			}
			else
			{
				// Mongoose docs are wrapped and not mutable for purposes of being serialized
				game = game.toJSON();
				game.full = game.seatsLeft == 0 ? true : false;
				delete game.seatsLeft;

				res.json(game);
			}
		});
	});

	app.post('/game/:id', loadUser, function(req, res) {
		Game.findOne({_id: mongoose.Types.ObjectId(req.params.id)}, function (err, game) {
			if (err)
			{
				console.log(err);
				res.json(err);
			}
			else
			{
				if (game.seatsLeft == 0)
				{
					res.json(
					{
						success: false,
						message: 'Game is full'
					});
				}
				// Check if user has already reserved a ticket
				else
				{
					for (ticket in req.currentUser.tickets)
					{
						if (game._id.equals(req.currentUser.tickets[ticket].game_id)){
							res.json(
							{
								success: false,
								message: 'Already have a ticket for this game'
							});
							return;
						}
					}

					game.seatsLeft -= 1;
					game.save();
					var newTicket = {
						game_id: game._id,
						confirmationId: '751263',
						seat: 'B',
						row: '42'
					};
					var newTickets = req.currentUser.tickets;
					newTickets.push(newTicket);
					req.currentUser.tickets = newTickets;
					req.currentUser.save();
					res.json(
					{
						success: true
					});
				}
			}
		});
	});

	http.createServer(app).listen(80);
	https.createServer(options, app).listen(443);
	console.log('Seminole Tix REST Server listening on port 80 and 443');
});
