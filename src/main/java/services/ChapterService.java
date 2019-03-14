
package services;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Validator;

import repositories.ChapterRepository;
import security.Authority;
import security.UserAccount;
import domain.Box;
import domain.Chapter;
import domain.Proclaim;
import domain.SocialProfile;
import forms.FormObjectChapter;

@Service
@Transactional
public class ChapterService {

	//Managed repository ---------------------------------

	@Autowired
	private ChapterRepository	chapterRepository;

	//Supporting services --------------------------------

	@Autowired
	private BoxService			boxService;

	@Autowired
	private ActorService		actorService;

	@Autowired
	private Validator			validator;


	//Simple CRUD Methods --------------------------------

	public Chapter create() {
		final Authority a = new Authority();
		a.setAuthority(Authority.CHAPTER);
		final UserAccount account = new UserAccount();
		account.setAuthorities(Arrays.asList(a));
		account.setBanned(false);

		final Chapter chapter = new Chapter();
		chapter.setSpammer(false);
		chapter.setSocialProfiles(new ArrayList<SocialProfile>());
		chapter.setUserAccount(account);
		chapter.setBoxes(new ArrayList<Box>());

		chapter.setProclaims(new ArrayList<Proclaim>());

		return chapter;
	}

	public Collection<Chapter> findAll() {
		return this.chapterRepository.findAll();
	}

	public Chapter findOne(final int id) {
		Assert.notNull(id);

		return this.chapterRepository.findOne(id);
	}

	public Chapter save(final Chapter chapter) {
		Assert.notNull(chapter);
		Chapter saved2;

		//Assertion that the email is valid according to the checkAdminEmail method.
		Assert.isTrue(this.actorService.checkUserEmail(chapter.getEmail()));

		//Assertion to check that the address isn't just a white space.
		Assert.isTrue(this.actorService.checkAddress(chapter.getAddress()));

		//Assertion that the phone is valid according to the checkPhone method.
		Assert.isTrue(this.actorService.checkPhone(chapter.getPhone()));

		//Checking if the actor is bannable according to the "bannableActors" query.
		if (this.actorService.isBannable(chapter) == true)
			chapter.setSpammer(true);

		if (chapter.getId() != 0) {
			Assert.isTrue(this.actorService.findByPrincipal().getId() == chapter.getId());
			saved2 = this.chapterRepository.save(chapter);
		} else {
			final Chapter saved = this.chapterRepository.save(chapter);
			this.actorService.hashPassword(saved);
			saved.setBoxes(this.boxService.generateDefaultFolders(saved));
			saved2 = this.chapterRepository.save(saved);
		}

		return saved2;
	}

	public void delete(final Chapter chapter) {
		Assert.notNull(chapter);

		//Assertion that the user deleting this brotherhood has the correct privilege.
		Assert.isTrue(this.actorService.findByPrincipal().getId() == chapter.getId());

		this.chapterRepository.delete(chapter);
	}

	//Reconstruct

	public Chapter reconstruct(final FormObjectChapter fob, final BindingResult binding) {
		final Chapter result = this.create();

		Assert.isTrue(fob.getAcceptedTerms());
		Assert.isTrue(fob.getPassword().equals(fob.getSecondPassword()));

		result.setName(fob.getName());
		result.setMiddleName(fob.getMiddleName());
		result.setSurname(fob.getSurname());
		result.setPhoto(fob.getPhoto());
		result.setEmail(fob.getEmail());
		result.setPhone(fob.getPhone());
		result.setAddress(fob.getAddress());
		result.setTitle(fob.getTitle());
		result.getUserAccount().setUsername(fob.getUsername());
		result.getUserAccount().setPassword(fob.getPassword());

		this.validator.validate(result, binding);

		//Assertion that the email is valid according to the checkAdminEmail method.
		Assert.isTrue(this.actorService.checkUserEmail(result.getEmail()));

		//Assertion to check that the address isn't just a white space.
		Assert.isTrue(this.actorService.checkAddress(result.getAddress()));

		//Assertion that the phone is valid according to the checkPhone method.
		Assert.isTrue(this.actorService.checkPhone(result.getPhone()));

		return result;

	}

	public Chapter reconstructPruned(final Chapter chapter, final BindingResult binding) {
		Chapter result;

		result = this.chapterRepository.findOne(chapter.getId());

		result.setName(chapter.getName());
		result.setMiddleName(chapter.getMiddleName());
		result.setSurname(chapter.getSurname());
		result.setPhoto(chapter.getPhoto());
		result.setEmail(chapter.getEmail());
		result.setPhone(chapter.getPhone());
		result.setAddress(chapter.getAddress());
		result.setTitle(chapter.getTitle());
		if (result.getArea() == null)
			result.setArea(chapter.getArea());
		this.validator.validate(result, binding);

		Assert.isTrue(this.actorService.findByPrincipal().getId() == result.getId());

		//Assertion that the email is valid according to the checkAdminEmail method.
		Assert.isTrue(this.actorService.checkUserEmail(result.getEmail()));

		//Assertion to check that the address isn't just a white space.
		Assert.isTrue(this.actorService.checkAddress(result.getAddress()));

		//Assertion that the phone is valid according to the checkPhone method.
		Assert.isTrue(this.actorService.checkPhone(result.getPhone()));

		return result;

	}

	//Other methods

	//Returns the enrolmentable brotherhoods given a member id
	public Collection<Chapter> chapterWithMoreParadesApprovedThanAvg() {
		return this.chapterRepository.chapterWithMoreParadesApprovedThanAvg();
	}

}
