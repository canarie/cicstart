package ca.ualberta.physics.cssdp.model;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Sets;
import com.google.common.primitives.Chars;
import com.wordnik.swagger.annotations.ApiClass;
import com.wordnik.swagger.annotations.ApiProperty;

/**
 * A Mnemonic is used to refer to entities of meta-data in the system. They make
 * up external_keys that can be used to lookup such things as Sites,
 * Deployments, Nodes, and Sensors. For example, the Chamela site in Mexico
 * would have the
 */
@ApiClass(value = "A pattern of letters, numbers, and characters that assists in remembering something.", description = "Max 20 chars.  Allow")
public class Mnemonic extends Model {

	private static final long serialVersionUID = 1L;

	public static final Set<Character> allowedChars = new HashSet<Character>(
			Arrays.asList('a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j',
					'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v',
					'w', 'x', 'y', 'z', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H',
					'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T',
					'U', 'V', 'W', 'X', 'Y', 'Z', '0', '1', '2', '3', '4', '5',
					'6', '7', '8', '9', '-', '_', '+', '.', '/'));

	// TODO make this a property
	public static int maxLength = 20;

	@ApiProperty(value = "Max length = 20.  Allowed chars [a-z0-9\\-_+./]", required = true)
	private String value;

	public Mnemonic() {

	}

	public Mnemonic(String value) {
		validate(value);
		this.value = value;
	}

	private void validate(String value) {
		List<Character> characterList = Chars.asList(value.toCharArray());
		Set<Character> characterSet = new HashSet<Character>(characterList);

		Set<Character> shouldBeEmpty = Sets.difference(characterSet,
				allowedChars);
		if (shouldBeEmpty.size() > 0) {
			throw new IllegalArgumentException(
					"Mnemonic contains invalid characters: " + shouldBeEmpty
							+ " \n Only these are allowed: " + allowedChars);
		}

		if (value.length() > 20) {
			throw new IllegalArgumentException(
					"Mnemonic length is too long, max is 20 characters");
		}
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		validate(value);
		this.value = value;
	}

	@Override
	public String _pk() {
		return value;
	}

	@Override
	public String toString() {
		return getValue();
	}

	public static Mnemonic of(String value) {
		return new Mnemonic(value);
	}

}